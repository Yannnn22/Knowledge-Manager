package zjc.knowledgemanager.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zjc.knowledgemanager.dto.TagDTO;
import zjc.knowledgemanager.entity.Tag;
import zjc.knowledgemanager.mapper.TagMapper;
import zjc.knowledgemanager.service.TagService;
import zjc.knowledgemanager.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public TagDTO createTag(TagDTO tagDTO) {
        Tag existingTag = tagMapper.findByName(tagDTO.getName());
        if (existingTag != null && existingTag.getIsDeleted() == 0) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setId(idGenerator.nextId());
        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor());
        tag.setDescription(tagDTO.getDescription());
        tag.setCreatorId(tagDTO.getCreatorId());
        tag.setUsageCount(0);
        tag.setIsDeleted(0);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());

        tagMapper.insert(tag);
        return convertToDTO(tag);
    }

    @Override
    public List<TagDTO> getAllTags() {
        List<Tag> tags = tagMapper.findAll();
        return tags.stream()
                .filter(tag -> tag.getIsDeleted() == 0)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TagDTO getTagById(Long id) {
        Tag tag = tagMapper.findById(id);
        if (tag == null || tag.getIsDeleted() == 1) {
            return null;
        }
        return convertToDTO(tag);
    }

    @Override
    public TagDTO updateTag(Long id, TagDTO tagDTO, Long userId) {
        Tag tag = tagMapper.findById(id);
        if (tag == null || tag.getIsDeleted() == 1) {
            throw new IllegalArgumentException("标签不存在");
        }

        if (!tag.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改此标签");
        }

        Tag existingTag = tagMapper.findByName(tagDTO.getName());
        if (existingTag != null && !existingTag.getId().equals(id) && existingTag.getIsDeleted() == 0) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        tag.setName(tagDTO.getName());
        tag.setColor(tagDTO.getColor());
        tag.setDescription(tagDTO.getDescription());
        tag.setUpdateTime(LocalDateTime.now());

        tagMapper.update(tag);
        return convertToDTO(tag);
    }

    @Override
    public boolean deleteTag(Long id, Long userId) {
        Tag tag = tagMapper.findById(id);
        if (tag == null || tag.getIsDeleted() == 1) {
            return false;
        }

        if (!tag.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除此标签");
        }

        tagMapper.deleteById(id);
        return true;
    }

    private TagDTO convertToDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        dto.setDescription(tag.getDescription());
        dto.setCreatorId(tag.getCreatorId());
        dto.setUsageCount(tag.getUsageCount());
        return dto;
    }
}