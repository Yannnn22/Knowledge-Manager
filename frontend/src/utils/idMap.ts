// 提供临时的知识库ID映射与规范化
// 说明：后端当前使用简单的数值ID（如 1、2、3），而部分前端来源可能是长字符串ID
// 这里做一个最小可行的规范化，以确保文档检索/上传能命中后端数据

const KB_ID_MAP: Record<string, string> = {
  // 根据API.md中的数据库结构，正确的映射关系：
  // 数据库中的知识库：
  // ID 1 → "技术文档库"
  // ID 2 → "产品手册库" 
  // ID 3 → "培训资料库"
  
  // 前端长ID -> 数据库ID 的映射（根据实际情况调整）
  '19337107299766272': '1',  // 技术文档库
  '19232187194019840': '2'   // 产品手册库（之前错误地标记为测试知识库2）
}

export const normalizeKnowledgeBaseId = (id: string | number | undefined | null): string => {
  if (id === undefined || id === null) return ''
  const str = String(id).trim()
  if (!str) return ''
  // 命中已知映射
  if (KB_ID_MAP[str]) return KB_ID_MAP[str]
  // 已经是纯数字
  if (/^\d+$/.test(str)) return str
  // 去掉可能的前缀（如 kb123、KB123）
  const noPrefix = str.replace(/^kb/i, '')
  if (/^\d+$/.test(noPrefix)) return noPrefix
  // 其他情况按原样返回（后端若不识别会返回空）
  return str
}

export const normalizeKnowledgeBaseIds = (ids: Array<string | number> | undefined | null): string[] => {
  if (!ids || !Array.isArray(ids)) return []
  return ids.map((x) => normalizeKnowledgeBaseId(x)).filter((x) => x !== '')
}


