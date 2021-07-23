package br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;

import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode( exclude = { "quoteSettings", "initialTimestamp" })
public abstract class BrokerRequest<T> {
	
	@Id
	protected String id;
	
	@Indexed(direction = IndexDirection.DESCENDING)
	protected LocalDateTime date;
	
	@Indexed(direction = IndexDirection.DESCENDING)
	protected long initialTimestamp;
	
	protected LocalDateTime finalDate;
	
	protected long finalTimestamp;
	
	protected long diffTimestamp;
	
	@Indexed
	protected String xApplicationName;
	protected String xCurrentDate;
	
	protected String xLocale;
	protected String companyId;
	protected String errorMessage;
	
	//@Indexed
	protected boolean hasErrorMessage;
	//@Indexed
	protected boolean hasExceptions;
	
	protected String errorStack;
	
	protected QuoteSettings quoteSettings;

	@Indexed
	private String traceId;
	private String transactionId;
	
	private T response;
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private Map<String, List<Pair>> exceptions;
	
	public Map<String, List<Pair>> getExceptions() {
		return exceptions;
	}
	
	public void addException(String name, Throwable throwable) {
		if (exceptions == null)
			exceptions = new ConcurrentHashMap<>();
		
		List<Pair> list = exceptions.computeIfAbsent(name, k -> new ArrayList<>());
		
		list.add(new Pair(throwable.getMessage(), this.stackToString(throwable.getStackTrace())));
	}

	public void addException(String name, List<BrokerRequest<DeliveryOptionsReturn>.Pair> pair) {
		if (exceptions == null)
			exceptions = new ConcurrentHashMap<>();
		
		List<Pair> list = exceptions.computeIfAbsent(name, k -> new ArrayList<>());
		
		pair.forEach(p -> list.add(new Pair(p.getMessage(), p.getStackTrace())));		
	}
	
	public String stackToString(StackTraceElement[] stackTrace) {
		if (stackTrace == null)
			return "[null]";
		
		if (stackTrace.length == 0)
			return "[empty]";
		
		StringBuilder stack = new StringBuilder();
		
		for (StackTraceElement e : stackTrace)
			stack.append(e.toString() + "\n");
		
		return stack.toString();
	}

	public void setErrorFlags(){
		hasExceptions = exceptions != null && !exceptions.isEmpty();
		hasErrorMessage = errorMessage != null && !errorMessage.equals("");
	}
	
	public class Pair {
		private String message;
		private String stackTrace;
		
		public Pair(String message, String stackTrace) {
			this.message = message;
			this.stackTrace = stackTrace;
		}
		
		public String getMessage() {
			return message;
		}
		
		public String getStackTrace() {
			return stackTrace;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
		
		public void setStackTrace(String stackTrace) {
			this.stackTrace = stackTrace;
		}
	}

	public void registerFinalTimestamp() {
		setFinalDate(LocalDateTime.now());
		setFinalTimestamp(System.currentTimeMillis());
		setDiffTimestamp(getFinalTimestamp() - getInitialTimestamp());
	}

}
