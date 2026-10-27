import http from '@/utils/http';
import { useAuthStore } from '@/stores/auth';

// 定义聊天请求参数的接口
export interface ChatRequestParams {
  prompt: string;
  topK?: number;
  threshold?: number;
  fileId?: string;
  model?: string;
  temperature?: number;
  chatId?: string;
}

// 定义非流式聊天的响应数据接口
export interface ChatOnceResponse {
  answer: string;
  related_docs: Array<{
    documentId: string;
    title: string;
    score: number;
  }>;
}

function normalizeChatOnceResponse(raw: any): ChatOnceResponse {
  if (!raw) return { answer: '', related_docs: [] };
  const payload = (raw && typeof raw === 'object' && 'data' in raw && raw.data) ? raw.data : raw;
  const answer = payload?.answer ?? payload?.content ?? payload?.text ?? (typeof payload === 'string' ? payload : '') ?? '';
  const related = payload?.related_docs ?? payload?.references ?? payload?.docs ?? [];
  const related_docs = Array.isArray(related)
    ? related.map((d: any) => ({
        documentId: String(d?.documentId ?? d?.id ?? d?.docId ?? ''),
        title: String(d?.title ?? d?.name ?? d?.filename ?? ''),
        score: Number(d?.score ?? d?.similarity ?? 0),
      }))
    : [];
  return { answer: String(answer), related_docs };
}

/**
 * RAG 非流式聊天（用于调试）
 * @param params - 聊天请求参数
 * @returns 一次性返回的完整答案
 */
export const getChatOnce = async (params: ChatRequestParams, retry = 1): Promise<{ data: ChatOnceResponse }> => {
  const { model, temperature, chatId, ...queryParams } = params;
  const headers: Record<string, string> = {};
  if (model) headers['model'] = model;
  if (temperature) headers['temperature'] = String(temperature);
  if (chatId) headers['chatId'] = chatId;

  // 后端AI接口实际为 /ai/**，需绕过全局 baseURL('/api')，使用独立前缀
  // 使用 text 响应类型并提升超时，兼容后端返回纯文本或JSON
  try {
    const raw = await http.get('/ai/chat-once', {
      params: queryParams,
      headers,
      baseURL: '/',
      responseType: 'text',
      timeout: 35000,
    }) as any;

    let resp: any = raw;
    try {
      if (typeof raw === 'string') {
        resp = JSON.parse(raw);
      }
    } catch {
      resp = raw; // 保持为文本
    }
    const data = normalizeChatOnceResponse(resp);
    return { data };
  } catch (err) {
    if (retry > 0) {
      await new Promise(r => setTimeout(r, 800));
      // 第二次尝试放宽超时
      const raw = await http.get('/ai/chat-once', {
        params: queryParams,
        headers,
        baseURL: '/',
          responseType: 'text',
        timeout: 60000,
      }) as any;
      let resp: any = raw;
      try {
        if (typeof raw === 'string') resp = JSON.parse(raw);
      } catch { resp = raw; }
      const data = normalizeChatOnceResponse(resp);
      return { data };
    }
    throw err;
  }
};

/**
 * RAG 流式聊天 (SSE via fetch + ReadableStream)
 * 使用 fetch 替代 EventSource，支持自定义 Header（Authorization、model 等）
 * @param params - 聊天请求参数
 * @param onData - 接收到数据块时的回调函数
 * @param onError - 发生错误时的回调函数
 * @param onDone - 聊天完成时的回调函数
 * @returns 一个可以用来中止请求的 AbortController
 */
export const getChatStream = (
  params: ChatRequestParams,
  onData: (chunk: string) => void,
  onError: (error: Error) => void,
  onDone: () => void
): { abort: () => void } => {
  const { model, temperature, chatId, ...queryParams } = params;

  // 所有业务参数放入 query params（手动编码中文参数）
  const qsParts: string[] = [];
  if (queryParams.prompt) qsParts.push(`prompt=${encodeURIComponent(queryParams.prompt)}`);
  if (queryParams.topK != null) qsParts.push(`topK=${queryParams.topK}`);
  if (queryParams.threshold != null) qsParts.push(`threshold=${queryParams.threshold}`);
  if (queryParams.fileId) qsParts.push(`fileId=${queryParams.fileId}`);
  if (chatId) qsParts.push(`chatId=${chatId}`);
  if (model) qsParts.push(`model=${encodeURIComponent(model)}`);
  if (temperature != null) qsParts.push(`temperature=${temperature}`);

  const url = `/ai/chat-gated?${qsParts.join('&')}`;

  // 构建请求头：同时通过 Header 传递参数（兼容后端 @RequestHeader）
  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
  };

  // 注入认证 token（Pinia 自动解包 ref，localStorage 作为 fallback）
  const authStore = useAuthStore();
  const token = (authStore as any).token || localStorage.getItem('token') || localStorage.getItem('userToken');
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (model) headers['model'] = model;
  if (temperature != null) headers['temperature'] = String(temperature);
  if (chatId) headers['chatId'] = chatId;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => {
    controller.abort();
    onError(new Error('请求超时，请稍后重试'));
  }, 300000);

  fetch(url, { headers, signal: controller.signal })
    .then(response => {
      clearTimeout(timeoutId);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      function read(): Promise<void> {
        return reader.read().then(({ done, value }) => {
          if (done) {
            if (buffer.trim()) {
              parseSSE(buffer, onData, onDone);
            } else {
              onDone();
            }
            return;
          }
          buffer += decoder.decode(value, { stream: true });
          // SSE 事件以 \n\n 分割
          const events = buffer.split('\n\n');
          buffer = events.pop() || '';
          for (const event of events) {
            parseSSE(event, onData, onDone);
          }
          return read();
        });
      }
      return read();
    })
    .catch(err => {
      clearTimeout(timeoutId);
      onError(err);
    });

  return { abort: () => controller.abort() };
};

/** 解析单个 SSE 事件块 */
function parseSSE(
  eventBlock: string,
  onData: (chunk: string) => void,
  onDone: () => void
) {
  const lines = eventBlock.split('\n');
  let eventType = '';
  let data = '';
  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      data += line.slice(5).trim();
    }
  }
  if (eventType === 'done' || data === '[DONE]') {
    onDone();
    return;
  }
  if (data) {
    onData(data);
  }
}

export interface AssistantStats {
  todayChatCount: number;
  todayMessageCount: number;
  totalChatCount: number;
  monthChatCount: number;
  accuracyRate: number;
  satisfactionRate: number;
  taskCompleted: number;
  taskInProgress: number;
}

export const getAssistantStats = async (): Promise<AssistantStats> => {
  const res: any = await http.get('/ai/stats', {
    baseURL: '/',
  });
  return res;
};

/**
 * 上传文件进行向量化入库，返回 fileId
 */
export const ingestUpload = async (file: File): Promise<string> => {
  const form = new FormData();
  form.append('file', file);
  const res = await fetch('/ingest/upload', { method: 'POST', body: form });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`上传失败(${res.status}): ${text || res.statusText}`);
  }
  const text = await res.text();
  const m = text.match(/fileId:\s*([a-z0-9-]+)/i);
  if (!m) throw new Error('未解析到 fileId');
  return m[1];
};