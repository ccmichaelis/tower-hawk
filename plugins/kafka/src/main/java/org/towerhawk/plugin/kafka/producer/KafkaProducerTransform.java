package org.towerhawk.plugin.kafka.producer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.towerhawk.monitor.check.evaluation.transform.Transform;
import org.towerhawk.monitor.descriptors.Keyable;
import org.towerhawk.monitor.descriptors.Partitionable;
import org.towerhawk.monitor.descriptors.Routable;
import org.towerhawk.monitor.descriptors.Timestampable;
import org.towerhawk.serde.resolver.TowerhawkType;

import java.util.Map;

@Getter
@Setter
@Slf4j
@TowerhawkType("kafkaProducerTransform")
public class KafkaProducerTransform implements Transform {

	@Setter(AccessLevel.NONE)
	@JsonIgnore
	private ObjectMapper mapper = new ObjectMapper();
	@Setter(AccessLevel.NONE)
	private Producer<String, String> producer;
	private String topic;
	private boolean deepUnwrap;
	private boolean unwrap;
	private transient int partitionCount;


	@JsonCreator
	public KafkaProducerTransform(
			@JsonProperty("props") Map<String, Object> props,
			@JsonProperty("topic") @NonNull String topic,
			@JsonProperty("unwrap") Boolean unwrap,
			@JsonProperty("deepUnwrap") Boolean deepUnwrap
	) {
		this.topic = topic;
		this.unwrap = unwrap == null ? true : unwrap;
		this.deepUnwrap = deepUnwrap == null ? false : deepUnwrap;
		props = ProducerPropsEnhancer.supplementProps(props);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, "towerhawk-consumer-lag");

		this.producer = new KafkaProducer<>(props);
		partitionCount = producer.partitionsFor(topic).size();
	}

	@Override
	public Object transform(Object value) {
		if (unwrap && value instanceof Map) {
			((Map<?, ?>) value).forEach((k, v) -> send(v));
		} else if (unwrap && value instanceof Iterable) {
			((Iterable<?>) value).forEach(this::send);
		} else {
			send(value);
		}
		producer.flush();
		return value;
	}

	private void send(Object value) {
		if (deepUnwrap && value instanceof Map) {
			((Map<?, ?>) value).forEach((k, v) -> transform(v));
			return;
		} else if (deepUnwrap && value instanceof Iterable) {
			((Iterable<?>) value).forEach(this::transform);
			return;
		}

		try {
			String key = "";
			Integer partition = null;
			long timestamp;
			String topic = this.topic;
			if (value instanceof Keyable) {
				key = ((Keyable) value).getKey();
			} else if (value instanceof Partitionable) {
				key = ((Partitionable) value).getPartitionKey();
				partition = ((Partitionable) value).getPartition(partitionCount);
			}
			if (value instanceof Timestampable) {
				timestamp = ((Timestampable) value).getTimestamp();
			} else {
				timestamp = System.currentTimeMillis();
			}
			if (value instanceof Routable) {
				topic = ((Routable) value).getQueueName();
			}
			topic = topic == null ? this.topic : topic;

			String val = mapper.writeValueAsString(value);
			ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, partition, timestamp, key, val);
			producer.send(producerRecord);
		} catch (JsonProcessingException e) {
			log.error("Unable to process record {}", value.toString(), e);
		}
	}
}