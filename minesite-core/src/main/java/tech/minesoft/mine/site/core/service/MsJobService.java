package tech.minesoft.mine.site.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.minesoft.mine.site.core.mapper.MsJobMapper;
import tech.minesoft.mine.site.core.mapper.MsJobRecordMapper;
import tech.minesoft.mine.site.core.models.MsJob;
import tech.minesoft.mine.site.core.models.MsJobRecord;
import tech.minesoft.mine.site.core.utils.Ctx;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

@Service
public class MsJobService {

    @Autowired
    public MsJobMapper jobMapper;

    @Autowired
    public MsJobRecordMapper recordMapper;

    public void run(String jobCode) {
        MsJob msJob = jobMapper.selectByCode(jobCode);

        int status = 0;
        String msg = "";
        if (msJob != null && 1 == msJob.getStatus()) {
            try {
                Object job = Ctx.getBean(msJob.getJobClz());
                Method method = job.getClass().getMethod(msJob.getJobMethod());
                method.invoke(job);

                status = 1;
            }catch (Exception e){
                msg = e.getMessage();
            }

            msJob.setLastStatus(status);
            msJob.setLastTime(new Date());
            jobMapper.updateByPrimaryKey(msJob);

            MsJobRecord record = new MsJobRecord();
            record.setJobId(msJob.getId());
            record.setJobName(msJob.getJobName());
            record.setStatus(status);
            record.setMsg(msg);
            record.setCreateTime(new Date());
            recordMapper.insert(record);
        }
    }

    public List<MsJob> loadAll() {
        return jobMapper.selectAll();
    }

    public MsJob select(Integer id) {
        return jobMapper.selectByPrimaryKey(id);
    }

    public void delete(Integer id) {
        jobMapper.deleteByPrimaryKey(id);
    }

    public void add(MsJob job) {
        job.setCreateTime(new Date());
        jobMapper.insert(job);
    }

    public void modify(MsJob job) {
        MsJob old = jobMapper.selectByPrimaryKey(job.getId());

        old.setJobCode(job.getJobCode());
        old.setJobClz(job.getJobClz());
        old.setJobMethod(job.getJobMethod());
        old.setJobName(job.getJobName());
        old.setStatus(job.getStatus());

        jobMapper.updateByPrimaryKey(old);
    }
}
