package cn.sixlab.mine.site.core.mapper;

import cn.sixlab.mine.site.core.models.MsJobRecord;

public interface MsJobRecordMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(MsJobRecord record);

    int insertSelective(MsJobRecord record);

    MsJobRecord selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(MsJobRecord record);

    int updateByPrimaryKeyWithBLOBs(MsJobRecord record);

    int updateByPrimaryKey(MsJobRecord record);
}