package org.towerhawk.monitor.check.evaluation.threshold.constant;

import org.towerhawk.monitor.check.run.CheckRun;
import org.towerhawk.monitor.check.evaluation.threshold.Threshold;
import org.towerhawk.serde.resolver.TowerhawkType;

@TowerhawkType("error")
public class ErrorThreshold implements Threshold {

	@Override
	public void evaluate(CheckRun.Builder builder, String key, Object value, boolean setMessage, boolean addContext) throws Exception {
		throw new Exception("Always thrown");
	}
}