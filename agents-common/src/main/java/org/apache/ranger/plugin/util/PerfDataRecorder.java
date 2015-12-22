/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class PerfDataRecorder {
	private static final Log LOG  = LogFactory.getLog(PerfDataRecorder.class);
	private static final Log PERF = RangerPerfTracer.getPerfLogger(PerfDataRecorder.class);

	static PerfDataRecorder instance = null;
	private Map<String, PerfStatistic> perfStatistics = new HashMap<String, PerfStatistic>();

	public static void initialize(List<String> names) {
		if (getPerfDataRecorder() == null) {
			instance = new PerfDataRecorder();
		}
		instance.init(names);
	}

	public static PerfDataRecorder getPerfDataRecorder() {
		return instance;
	}

	public void dumpStatistics() {
		for (Map.Entry<String, PerfStatistic> entry : perfStatistics.entrySet()) {

			String tag = entry.getKey();
			PerfStatistic perfStatistic = entry.getValue();

			long averageTimeSpent = 0L;
			long minTimeSpent = 0L;
			long maxTimeSpent = 0L;
			if (perfStatistic.numberOfInvocations.get() != 0L) {
				averageTimeSpent = perfStatistic.millisecondsSpent.get()/perfStatistic.numberOfInvocations.get();
				minTimeSpent = perfStatistic.minTimeSpent.get();
				maxTimeSpent = perfStatistic.maxTimeSpent.get();
			}

			String logMsg = "[" + tag + "]" +
                             " execCount:" + perfStatistic.numberOfInvocations +
                             ", totalTimeTaken:" + perfStatistic.millisecondsSpent +
                             ", maxTimeTaken:" + maxTimeSpent +
                             ", minTimeTaken:" + minTimeSpent +
                             ", avgTimeTaken:" + averageTimeSpent;

			LOG.info(logMsg);
			PERF.debug(logMsg);
		}
	}

	void record(String tag, long elapsedTime) {
		PerfStatistic perfStatistic = perfStatistics.get(tag);
		if (perfStatistic != null) {
			perfStatistic.addPerfDataItem(elapsedTime);
		}
	}

	private void init(List<String> names) {
		if (CollectionUtils.isNotEmpty(names)) {
			for (String name : names) {
				// Create structure
				perfStatistics.put(name, new PerfStatistic());
			}
		}
	}

	private class PerfStatistic {
		private AtomicLong numberOfInvocations = new AtomicLong(0L);
		private AtomicLong millisecondsSpent = new AtomicLong(0L);
		private AtomicLong minTimeSpent = new AtomicLong(Long.MAX_VALUE);
		private AtomicLong maxTimeSpent = new AtomicLong(Long.MIN_VALUE);

		void addPerfDataItem(final long timeTaken) {
			numberOfInvocations.getAndIncrement();
			millisecondsSpent.getAndAdd(timeTaken);

			long min = minTimeSpent.get();
			if (timeTaken < min) {
				minTimeSpent.compareAndSet(min, timeTaken);
			}

			long max = maxTimeSpent.get();
			if (timeTaken > max) {
				maxTimeSpent.compareAndSet(max, timeTaken);
			}
		}
	}
}
