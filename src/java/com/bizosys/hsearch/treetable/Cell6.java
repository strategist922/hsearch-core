package com.bizosys.hsearch.treetable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.bizosys.hsearch.byteutils.SortedByte;
import com.bizosys.hsearch.byteutils.SortedBytesArray;

public class Cell6<K1, K2, K3, K4, K5, V> extends CellBase<K1> {

	public SortedByte<K2> k2Sorter = null;
	public SortedByte<K3> k3Sorter = null;
	public SortedByte<K4> k4Sorter = null;
	public SortedByte<K5> k5Sorter = null;
	public SortedByte<V> vSorter = null;
	
	public Map<K1, Cell5<K2,K3,K4, K5, V>> sortedList;

	public Cell6 (SortedByte<K1> k1Sorter,SortedByte<K2> k2Sorter, SortedByte<K3> k3Sorter, 
			SortedByte<K4> k4Sorter, SortedByte<K5> k5Sorter, SortedByte<V> vSorter) {
		this.k1Sorter = k1Sorter;
		this.k2Sorter = k2Sorter;
		this.k3Sorter = k3Sorter;
		this.k4Sorter = k4Sorter;
		this.k5Sorter = k5Sorter;
		this.vSorter = vSorter;
	}
	
	public Cell6 (SortedByte<K1> k1Sorter,SortedByte<K2> k2Sorter, SortedByte<K3> k3Sorter,
			SortedByte<K4> k4Sorter, SortedByte<K5> k5Sorter,
			SortedByte<V> vSorter, Map<K1, Cell5<K2,K3,K4, K5, V>> sortedList ) {
		this(k1Sorter, k2Sorter, k3Sorter, k4Sorter, k5Sorter, vSorter);
		this.sortedList = sortedList;
	}

	public Cell6 (SortedByte<K1> k1Sorter,SortedByte<K2> k2Sorter, SortedByte<K3> k3Sorter,
			SortedByte<K4> k4Sorter, SortedByte<K5> k5Sorter, 
			SortedByte<V> vSorter, byte[] data ) {
		this(k1Sorter, k2Sorter, k3Sorter, k4Sorter, k5Sorter, vSorter);
		this.data = data;
	}

	public void add(K1 k1, K2 k2, K3 k3, K4 k4, K5 k5, V v) {
		if ( null == this.sortedList) this.sortedList = new TreeMap<K1, Cell5<K2,K3,K4, K5, V>>();
		
		Cell5<K2, K3, K4, K5, V> val = null;
		if ( this.sortedList.containsKey(k1)) val = sortedList.get(k1);
		else {
			val = new Cell5<K2, K3, K4, K5, V>(k2Sorter, k3Sorter, k4Sorter, k5Sorter, vSorter);
			sortedList.put(k1, val);
		}
		
		this.sortedList.put(k1, val);
		val.add(k2, k3, k4, k5, v);
	}
	
	public void sort(Comparator<CellKeyValue<K5, V>> comp) {
		if ( null == sortedList) return;
		for (Cell5<K2,K3,K4,K5,V> entry : sortedList.values()) {
			entry.sort(comp);
		}
	}		

	@Override
	protected List<byte[]> getEmbeddedCellBytes() throws IOException {
		List<byte[]> values = new ArrayList<byte[]>();
		for (Cell5<K2,K3, K4, K5, V> cell5 : sortedList.values()) {
			values.add(cell5.toBytes());
		}
		return values;
	}
	
	@Override
	protected byte[] getKeyBytes() throws IOException {
		return k1Sorter.toBytes(this.sortedList.keySet(), false);
	}

	
	public void findValues(K1 exactValue, K1 minimumValue, K1 maximumValue, 
			Collection<Cell5<K2, K3, K4, K5, V>> foundValues) throws IOException {

		List<Integer> foundPositions = new ArrayList<Integer>();
		findMatchingPositions(exactValue, minimumValue, maximumValue, foundPositions);

		SortedByte<byte[]> sortedBA = SortedBytesArray.getInstance();
		byte[] valuesB = sortedBA.getValueAt(data, 1);

		for (int position : foundPositions) {
			foundValues.add( new Cell5<K2, K3, K4, K5, V>(
				k2Sorter, k3Sorter, k4Sorter, k5Sorter, vSorter, sortedBA.getValueAt(valuesB, position)));
		}
	}
	
	public void getAllValues(Collection<Cell5<K2, K3, K4, K5, V>> values) throws IOException {
		SortedByte<byte[]> sortedBA = SortedBytesArray.getInstance();
		byte[] allValuesB = sortedBA.getValueAt(data, 1);
		
		int length = ( null == allValuesB) ? 0 : allValuesB.length;
		int size = sortedBA.getSize(allValuesB, 0, length);
		
		for ( int i=0; i<size; i++) {
			values.add( new Cell5<K2, K3, K4, K5, V>( k2Sorter, k3Sorter, k4Sorter, k5Sorter,  vSorter, 
				sortedBA.getValueAt(allValuesB, i)) );
		}
	}
	
	public void parseElements() throws IOException {
		if ( null == this.sortedList) this.sortedList = new TreeMap<K1, Cell5<K2,K3,K4,K5,V>>();
		else this.sortedList.clear();
		
		List<K1> allKeys = new ArrayList<K1>();
		List<Cell5<K2,K3,K4,K5,V>> allValues = new ArrayList<Cell5<K2,K3,K4,K5,V>>();
		
		getAllKeys(allKeys);
		getAllValues(allValues);
		int allKeysT = allKeys.size();
		if ( allKeysT != allValues.size() ) throw new IOException( 
			"Keys and Values tally mismatched : keys(" + allKeysT + ") , values(" + allValues.size() + ")");
		
		for ( int i=0; i<allKeysT; i++) {
			Cell5<K2, K3, K4, K5, V> aVal = allValues.get(i);
			sortedList.put(allKeys.get(i), aVal);
		}
	}

}