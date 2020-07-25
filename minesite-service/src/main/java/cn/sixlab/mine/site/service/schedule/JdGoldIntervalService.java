package cn.sixlab.mine.site.service.schedule;

import cn.sixlab.mine.site.common.utils.HttpUtils;
import cn.sixlab.mine.site.common.utils.JsonUtils;
import cn.sixlab.mine.site.common.vo.ResultJson;
import cn.sixlab.mine.site.data.mapper.MsDataMapper;
import cn.sixlab.mine.site.data.mapper.MsNotifyConfigMapper;
import cn.sixlab.mine.site.data.models.MsData;
import cn.sixlab.mine.site.data.models.MsNotifyConfig;
import cn.sixlab.mine.site.service.api.Job;
import cn.sixlab.mine.site.service.service.DingTalkService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class JdGoldIntervalService implements Job {
    private static String CODE = "jdGold";

    @Autowired
    private MsDataMapper dataDao;

    @Autowired
    private MsNotifyConfigMapper notifyConfigDao;

    @Autowired
    private DingTalkService dingTalkService;

    @Override
    public void run() {
        MsData lastData = dataDao.selectFirstByCode(CODE);

        String apiUrl = "https://ms.jr.jd.com/gw/generic/hj/h5/m/latestPrice";

        Map<String, String> header = new HashMap<>();
        header.put("host", "ms.jr.jd.com");
        header.put("referer", "https://m.jdjygold.com/finance-gold/msjgold/homepage?orderSource=7");

        ResultJson resultJson = HttpUtils.sendGet(apiUrl, null, header);
        if (resultJson.isSuccess()) {
            String msg = resultJson.getMessage();

            Map map = JsonUtils.toBean(msg, Map.class);

            Map resultData = MapUtils.getMap(map, "resultData");
            Map datas = MapUtils.getMap(resultData, "datas");

            Date time = new Date(MapUtils.getLong(datas, "time"));
            BigDecimal price = new BigDecimal(MapUtils.getString(datas, "price"));

            MsData msData = new MsData();
            msData.setCode(CODE);
            msData.setData(price);
            msData.setStatus(1);
            msData.setCreateTime(time);

            dataDao.insert(msData);

            String timeStr = DateFormatUtils.format(time, "yyyy-MM-dd HH:mm:ss");
            if (null == lastData) {
                StringBuilder builder = new StringBuilder();
                builder.append("开始监控黄金价格：\n日期：");
                builder.append(timeStr);
                builder.append("\n价格：");
                builder.append(price.toPlainString());
                builder.append("\n访问：https://datastrend.com/api/gold/index\n");

                dingTalkService.sendText(builder.toString());
            } else {
                List<MsNotifyConfig> configList = notifyConfigDao.queryAllByCode(CODE);

                if (CollectionUtils.isNotEmpty(configList)) {
                    for (MsNotifyConfig config : configList) {
                        if (warning(price, lastData.getData(), config)) {
                            StringBuilder builder = new StringBuilder();
                            builder.append("\n价格：");
                            builder.append(price.toPlainString());
                            builder.append("\n预警时间：");
                            builder.append(timeStr);
                            builder.append("\n访问：https://datastrend.com/api/gold/index\n");

                            dingTalkService.sendText(builder.toString());

                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean warning(BigDecimal now, BigDecimal last, MsNotifyConfig config) {
        Integer type = config.getType();
        BigDecimal rise = config.getRise();

        switch (type) {
            case 0:
                // 涨跌幅
                BigDecimal rised = now.subtract(last);

                if (rise.signum() == rised.signum()) {
                    return rised.abs().compareTo(rise.abs()) >= 0;
                }
                break;
            case 1:
            case -1:
                // 涨跌至
                return now.compareTo(rise) * type >= 0;
        }

        return false;
    }

}