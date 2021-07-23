package br.com.lojasrenner.rlog.transport.order.metrics;

import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class CombinationsTimeoutMetrics extends BaseMetrics {

    @Override
    protected String getMetricName() {
        return "combinationsTimeout";
    }

    public void sendCombinationsTimeOutMetrics(String companyId, String channel, Boolean isCombinationTimeOut) {
        Counter.Builder builder = beginCounter("bfl_delivery_options_combinations_timeout", "count of quotations with combinations timeout");
        addTag(companyId, channel, builder, "companyId", companyId);
        addTag(companyId, channel, builder, "channel", channel);
        addTag(companyId, channel, builder, "isCombinationTimeOut", isCombinationTimeOut != null ? isCombinationTimeOut.toString() : "false");
        Counter counter = register(companyId, channel, builder);
        increment(counter, 1);
    }
}
