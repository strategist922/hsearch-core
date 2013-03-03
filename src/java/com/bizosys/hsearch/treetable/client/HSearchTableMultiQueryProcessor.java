/*
* Copyright 2010 Bizosys Technologies Limited
*
* Licensed to the Bizosys Technologies Limited (Bizosys) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The Bizosys licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.bizosys.hsearch.treetable.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.bizosys.hsearch.federate.FederatedFacade;
import com.bizosys.hsearch.hbase.HbaseLog;

public abstract class HSearchTableMultiQueryProcessor implements IHSearchTableMultiQueryProcessor {

	public static boolean DEBUG_ENABLED = HbaseLog.l.isDebugEnabled();
	
	public final static List<com.bizosys.hsearch.federate.FederatedFacade<Long, String>.IRowId> noIdsFound = 
			new ArrayList<com.bizosys.hsearch.federate.FederatedFacade<Long, String>.IRowId>(0);
	
	public abstract IHSearchTableCombiner getCombiner();
	
	private FederatedFacade<Long, String> processor = null;
	
	public HSearchTableMultiQueryProcessor() {
		processor = build();
		processor.DEBUG_MODE = DEBUG_ENABLED;
	}
	
	public FederatedFacade<Long, String> getProcessor() { 
		return processor;
	}
	
	private FederatedFacade<Long, String> build() {

		return new FederatedFacade<Long, String>("", 
				HSearchTableResourcesDefault.getInstance().multiQueryIdObjectInitialCache,
				HSearchTableResourcesDefault.getInstance().multiQueryPartsThreads) {
			
			@Override
			public List<FederatedFacade<Long, String>.IRowId> populate(
					String type, String multiQueryPartId, String aStmtOrValue, Map<String, Object> stmtParams) throws IOException {

				if ( DEBUG_ENABLED ) L.getInstance().logDebug(  "HSearchTableMultiQuery.populate ENTER.");
				long startTime = System.currentTimeMillis();
				long endTime = -1L;
				try {
					IHSearchTableCombiner combiner = getCombiner(); 
					combiner.concurrentDeser(aStmtOrValue, stmtParams, type);

					IHSearchPlugin plugin = (IHSearchPlugin) stmtParams.get(HSearchTableMultiQueryExecutor.PLUGIN);
					Collection<String> keys = plugin.getUniqueRowKeys();
					if ( keys.size() == 0) {
						if ( DEBUG_ENABLED ) L.getInstance().logDebug(  "> " + "No Records found :");
						return noIdsFound;
					}
					if ( DEBUG_ENABLED ) L.getInstance().logDebug(  "> " + "Total Ids found :" + keys.size());
	
					List<com.bizosys.hsearch.federate.FederatedFacade<Long, String>.IRowId> results = 
							new ArrayList<com.bizosys.hsearch.federate.FederatedFacade<Long, String>.IRowId>(keys.size());
					
					for (String id : keys) {
						IRowId primary = objectFactory.getPrimaryKeyRowId(id);
						results.add(primary);
					}
					return results;
				} catch (Exception ex) {
					throw new IOException(ex);
				} finally {
					if ( DEBUG_ENABLED ) {
						endTime = System.currentTimeMillis();
						L.getInstance().logDebug( Thread.currentThread().getName() + "> " + "Deserialization Time :" + (endTime - startTime));
					}
				}
			}
		};
	}
}
