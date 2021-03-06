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

package com.bizosys.hsearch.treetable.storage;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.io.hfile.Compression;
import org.apache.hadoop.hbase.regionserver.StoreFile.BloomType;
import org.apache.log4j.Logger;

import com.bizosys.hsearch.hbase.HDML;

public class HBaseTableSchemaCreator {
	
	private static HBaseTableSchemaCreator instance = null;
	public static Logger l = Logger.getLogger(HBaseTableSchemaCreator.class.getName());
	
	public static final HBaseTableSchemaCreator getInstance() {
		if ( null != instance) return instance;
		synchronized (HBaseTableSchemaCreator.class) {
			if ( null != instance) return instance;
			instance = new HBaseTableSchemaCreator();
		}
		return instance;
	}
	
	/**
	 * Default constructor
	 *
	 */
	public HBaseTableSchemaCreator(){
	}
	
	private int partitionBlockSize = 13035596;	
	private int partitionRepMode = HConstants.REPLICATION_SCOPE_GLOBAL;;	

	/**
	 * Checks and Creates all necessary tables required for HSearch index.
	 */
	public boolean init() {

		try {
			
			List<HColumnDescriptor> colFamilies = new ArrayList<HColumnDescriptor>();
			
			HBaseTableSchemaDefn def = HBaseTableSchemaDefn.getInstance();
			for (String familyName : def.familyNames.keySet()) {
				
				//Partitioned
				for (String partition : def.familyNames.get(familyName)) {
					HColumnDescriptor rangeCols = new HColumnDescriptor( (familyName + "_" + partition ).getBytes());
					configColumn(rangeCols);
					colFamilies.add(rangeCols);
				}
				
				//No Partition
				if ( def.familyNames.get(familyName).size() == 0 ) {
					HColumnDescriptor rangeCols = new HColumnDescriptor( familyName.getBytes());
					configColumn(rangeCols);
					colFamilies.add(rangeCols);
				}
			}
			
			HDML.create(def.tableName, colFamilies);
			return true;
			
		} catch (Exception sf) {
			sf.printStackTrace(System.err);
			l.fatal(sf);
			return false;
		} 
	}

	/**
	 * Compression method to HBase compression code.
	 * @param methodName 
	 * @return
	 */
	public static String resolveCompression(String methodName) {
		String compClazz =  Compression.Algorithm.GZ.getName();
		if ("gz".equals(methodName)) {
			compClazz = Compression.Algorithm.GZ.getName();
		} else if ("lzo".equals(methodName)) {
			compClazz = Compression.Algorithm.LZO.getName();
		} else if ("none".equals(methodName)) {
			compClazz = Compression.Algorithm.NONE.getName();
		}
		return compClazz;
	}
	
	public void configColumn(HColumnDescriptor col) {
		col.setMinVersions(1);
		col.setMaxVersions(1);
		col.setKeepDeletedCells(false);
		col.setCompressionType(Compression.Algorithm.NONE);
		col.setEncodeOnDisk(false);
		col.setDataBlockEncoding(DataBlockEncoding.NONE);
		col.setInMemory(false);
		col.setBlockCacheEnabled(true);
		col.setBlocksize(partitionBlockSize);
		col.setTimeToLive(HConstants.FOREVER);
		col.setBloomFilterType(BloomType.NONE);
		col.setScope(partitionRepMode);
	}
}