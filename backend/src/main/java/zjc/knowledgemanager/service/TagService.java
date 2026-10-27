package zjc.knowledgemanager.service;

import zjc.knowledgemanager.dto.TagDTO;

import java.util.List;

public interface TagService {

    TagDTO createTag(TagDTO tagDTO);

    List<TagDTO> getAllTags();

    TagDTO getTagById(Long id);

    TagDTO updateTag(Long id, TagDTO tagDTO, Long userId);

    boolean deleteTag(Long id, Long userId);
}