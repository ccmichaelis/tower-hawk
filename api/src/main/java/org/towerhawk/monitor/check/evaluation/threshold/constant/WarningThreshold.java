package org.towerhawk.monitor.check.evaluation.threshold.constant;

import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.evaluation.threshold.Threshold;
import org.towerhawk.serde.resolver.TowerhawkType;

@TowerhawkType("warning")
public class WarningThreshold implements Threshold {

	@Override
	public void evaluate(CheckRun.Builder builder, String key, Object value, boolean setMessage, boolean addContext) throws Exception {
		builder.warning();
	}
}