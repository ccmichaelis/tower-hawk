package org.towerhawk.monitor.check.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.towerhawk.monitor.check.Check;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * This class is meant to amortize the cost of setting the MDC. While it
 * isn't necessarily expensive, keeping track of when it is set and isn't
 * can be rather difficult so it keeps track of how many times it has been
 * addResult and removed to make sure that it is only set when necessary and so
 * that a method won't remove the value from the MDC in while another method
 * is expecting it to be available.
 */
@Slf4j
public class CheckMDC {

	public static final String CHECK = "check";

	private static final ThreadLocal<Deque<Check>> threadLocal = ThreadLocal.withInitial(ArrayDeque::new);

	public static void put(Check check) {
		Deque<Check> deque = threadLocal.get();
		Check current = deque.peekLast();
		deque.addLast(check);
		if (current != check) {
			MDC.put(CHECK, check.getFullName());
		}
	}

	public static void remove() {
		Deque<Check> deque = threadLocal.get();
		Check current = deque.pollLast();
		Check next = deque.peekLast();
		if (next == null) {
			MDC.remove(CHECK);
		} else if (current != next) {
			MDC.put(CHECK, next.getFullName());
		}
	}
}
