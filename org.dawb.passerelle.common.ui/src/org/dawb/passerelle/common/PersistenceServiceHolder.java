package org.dawb.passerelle.common;

import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceServiceHolder {
	
	private static final Logger logger = LoggerFactory.getLogger(PersistenceServiceHolder.class);

	private IPersistenceService persistenceService;

	private static PersistenceServiceHolder current;
	
	public static PersistenceServiceHolder getInstance() {
		if (current==null) current = new PersistenceServiceHolder();
		return current;
	}

	public IPersistenceService getPersistenceService() {
		return persistenceService;
	}

	public void setPersistenceService(IPersistenceService persistenceService) {
		this.persistenceService = persistenceService;
		logger.debug("Set "+persistenceService.getClass().getSimpleName()+" in "+getClass().getSimpleName());
	}
	
	public void start(ComponentContext context) {
		current = this;
	}
	
	public void stop() {
		current = null;
	}
}
