/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gluu.oxtrust.config.OxTrustConfiguration;
import org.gluu.oxtrust.ldap.service.ApplianceService;
import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.model.AuthenticationChartDto;
import org.gluu.oxtrust.model.GluuOrganization;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.model.ApplicationType;
import org.xdi.model.metric.MetricType;
import org.xdi.model.metric.counter.CounterMetricEntry;
import org.xdi.model.metric.ldap.MetricEntry;
import org.xdi.service.CacheService;

/**
 * Store and retrieve metric
 *
 * @author Rahat Ali Date: 07/30/2015
 * @author Yuriy Movchan Date: 08/28/2015
 */
@Scope(ScopeType.STATELESS)
@Name(MetricService.METRIC_SERVICE_COMPONENT_NAME)
@AutoCreate
public class MetricService extends org.xdi.service.metric.MetricService {

	private static final long serialVersionUID = 7875838160379126796L;

	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	@Logger
	private Log log;

	@In
	private CacheService cacheService;

	@In
	private ApplianceService applianceService;

	@In
	private OrganizationService organizationService;

	@In
	private OxTrustConfiguration oxTrustConfiguration;

	public AuthenticationChartDto genereateAuthenticationChartDto(int countDays) {
		String key = OxTrustConstants.CACHE_METRICS_KEY + "#home";
		AuthenticationChartDto authenticationChartDto = (AuthenticationChartDto) cacheService.get(OxTrustConstants.CACHE_METRICS_NAME, key);
		if (authenticationChartDto != null) {
			return authenticationChartDto;
		}
		
		Map<MetricType, List<? extends MetricEntry>> entries = findAuthenticationMetrics(-countDays);

		String[] labels = new String[countDays];
		Map<String, Long> successStats = calculateCounterStatistics(countDays, (List<CounterMetricEntry>) entries.get(MetricType.OXAUTH_USER_AUTHENTICATION_SUCCESS));
		labels = successStats.keySet().toArray(labels);

		Long[] values = new Long[countDays];
		values = successStats.values().toArray(values);

		authenticationChartDto = new AuthenticationChartDto();

		authenticationChartDto.setLabels(labels);
		authenticationChartDto.setSuccess(values);

		Map<String, Long> failureStats = calculateCounterStatistics(countDays, (List<CounterMetricEntry>) entries.get(MetricType.OXAUTH_USER_AUTHENTICATION_FAILURES));
		values = new Long[countDays];
		values = failureStats.values().toArray(values);
		authenticationChartDto.setFailure(values);

		cacheService.put(OxTrustConstants.CACHE_METRICS_NAME, key, authenticationChartDto);

		return authenticationChartDto;
	}

	private Map<MetricType, List<? extends MetricEntry>> findAuthenticationMetrics(int countDays) {
		List<MetricType> metricTypes = new ArrayList<MetricType>();
		metricTypes.add(MetricType.OXAUTH_USER_AUTHENTICATION_FAILURES);
		metricTypes.add(MetricType.OXAUTH_USER_AUTHENTICATION_SUCCESS);

		Date endDate = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, countDays);

		Date startDate = calendar.getTime();

		Map<MetricType, List<? extends MetricEntry>> entries = findMetricEntry(ApplicationType.OX_AUTH, oxTrustConfiguration.getApplicationConfiguration()
				.getApplianceInum(), metricTypes, startDate, endDate);

		return entries;
	}

	private Map<String, Long> calculateCounterStatistics(int countDays, List<CounterMetricEntry> metrics) {
		// Prepare map with all dates
		Map<String, Long> stats = new TreeMap<String, Long>();
		Calendar calendar = Calendar.getInstance();
		for (int i = 0; i <= countDays; i++) {
			String dateString = df.format(calendar.getTime());
			stats.put(dateString, 0L);
			calendar.add(Calendar.DATE, -1);
		}

		if ((metrics == null) || (metrics.size() == 0)) {
			return stats;
		}

		// Detect servers restart and readjust counts
		// Server restart condition: previous entry CounterMetricEntry.CounterMetricEntry.count > current entry CounterMetricEntry.CounterMetricEntry.count
		CounterMetricEntry prevMetric = null;
		long prevDayCount = 0L;
		long adjust = 0;
		for (CounterMetricEntry metric : metrics) {
			Date date = metric.getCreationDate();
			calendar.setTime(date);

			// Detect server restarts
			if ((prevMetric != null) && (prevMetric.getMetricData().getCount() > metric.getMetricData().getCount() + adjust)) {
				// Last count before server restart
				long count = prevMetric.getMetricData().getCount();

				// Change adjust value
				adjust = count;
			}
			
			long count = metric.getMetricData().getCount();
			metric.getMetricData().setCount(count + adjust);
			
			prevMetric = metric;
		}

		// Iterate through ordered by MetricEntry.startDate list and just make value snapshot at the end of the day
		int prevDay = -1;
		prevMetric = null;
		prevDayCount = 0L;
		for (CounterMetricEntry metric : metrics) {
			Date date = metric.getCreationDate();
			calendar.setTime(date);

			int currDay = calendar.get(Calendar.DAY_OF_MONTH);
			if ((prevMetric != null) && (prevDay != currDay)) {
				long count = prevMetric.getMetricData().getCount();
				String dateString = df.format(prevMetric.getCreationDate());
				stats.put(dateString, count - prevDayCount);
				
				// Show only difference, not total
				prevDayCount = count;
			}
			
			prevMetric = metric;
			prevDay = currDay;
		}

		// Add last day statistic
		long count = prevMetric.getMetricData().getCount();
		String dateString = df.format(prevMetric.getCreationDate());
		stats.put(dateString, count - prevDayCount);

		return stats;
	}

	private void dump(List<CounterMetricEntry> metrics) {
		for (CounterMetricEntry metric : metrics) {
			Date date = metric.getCreationDate();
			long count = metric.getMetricData().getCount();
			System.out.println(date + " : " + count);
		}
	}

	@Override
	public String applianceInum() {
		return applianceService.getApplianceInum();
	}

	@Override
	public String getComponentName() {
		return METRIC_SERVICE_COMPONENT_NAME;
	}

	public static MetricService instance() {
		return (MetricService) Component.getInstance(MetricService.class);
	}

	@Override
	public String baseDn() {
		String orgDn = OrganizationService.instance().getDnForOrganization();
		String baseDn = String.format("ou=metric,%s", orgDn);
		return baseDn;
	}

}
