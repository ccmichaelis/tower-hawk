package org.towerhawk.monitor.check.evaluation.transform;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.towerhawk.serde.resolver.TransformType;

@Slf4j
@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@TransformType("regex")
public class RegexTransform implements Transform<String> {

	protected String regex;
	protected String replace;

	@Override
	public String transform(Object value) throws Exception {
		return value.toString().replaceAll(regex, replace);
	}
}