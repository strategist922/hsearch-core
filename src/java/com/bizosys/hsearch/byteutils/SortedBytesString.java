
package com.bizosys.hsearch.byteutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SortedBytesString extends SortedByte<String>{

	public static SortedByte<String> getInstance() {
		return new SortedBytesString();
	}
	
	private SortedBytesString() {
	}
	
	@Override
	public int getSize(byte[] bytes, int offset, int length) {
		if ( null == bytes) return 0;
		return Storable.getInt(offset, bytes);
	}
	
	/**
	 * 4 bytes - total entities
	 * 4 bytes - outputBytesLen ( total * ( 4 + string length) )
	 * Each element bytes length
	 * Each element bytes
	 */
	@Override
	public byte[] toBytes(Collection<String> sortedCollection, boolean clearList)
			throws IOException {

		//Total collection size, element start location, End Location
		byte[] headerBytes = new byte[4 + sortedCollection.size() * 4 + 4] ;
		System.arraycopy(Storable.putInt(sortedCollection.size()), 0, headerBytes, 0, 4);
		int offset = 4;  //4 is added for array size
		
		int outputBytesLen = 0;
		for (String bytes : sortedCollection) {

			//Populate header
			System.arraycopy(Storable.putInt(outputBytesLen), 0, headerBytes, offset, 4);
			offset = offset + 4;
			
			//Calculate Next Chunk length
			int bytesLen = ( null == bytes ) ? 0 : bytes.getBytes().length;
			outputBytesLen = outputBytesLen + bytesLen ;
			
		}
		System.arraycopy(Storable.putInt(outputBytesLen), 0, headerBytes, offset, 4);
		
		outputBytesLen = outputBytesLen + headerBytes.length; 
		byte[] outputBytes = new byte[outputBytesLen];
		System.arraycopy(headerBytes, 0, outputBytes, 0, headerBytes.length);
		offset = headerBytes.length;
		
		for (String bytes : sortedCollection) {
			int byteSize = ( null == bytes) ? 0 : bytes.getBytes().length;
			System.arraycopy(bytes.getBytes(), 0, outputBytes, offset, byteSize);
			offset = offset + byteSize;
		}
		return outputBytes;
	}

	@Override
	public void addAll(byte[] inputBytes, Collection<String> vals) throws IOException {
		addAll(inputBytes, 0, vals);
	}

	@Override
	public void addAll(byte[] inputBytes, int offset, Collection<String> vals) throws IOException {
		
		int collectionSize = Storable.getInt(offset, inputBytes);
		
		List<Integer> offsets = new ArrayList<Integer>();
		offset = offset + 4;
		
		for ( int i=0; i<collectionSize; i++) {
			int bytesLen = Storable.getInt( offset, inputBytes);
			offset = offset + 4;
			offsets.add(bytesLen);
		}
		offset = offset + 4;

		int headerOffset = offset;
		offsets.add( inputBytes.length - headerOffset);
		
		Integer nextElemOffset = -1;
		Integer thisElemOffset = -1;
		for ( int i=0; i<collectionSize; i++) {
			nextElemOffset = offsets.get(i+1);
			thisElemOffset = offsets.get(i);
			byte[] aElem = new byte[ nextElemOffset - thisElemOffset ];
			System.arraycopy(inputBytes, headerOffset + thisElemOffset, aElem, 0, aElem.length);
			vals.add( new String(aElem) );
		}		
	}

	@Override
	public String getValueAt(byte[] inputBytes, int pos) throws IOException {
		return getValueAt(inputBytes, 0, pos);
	}

	@Override
	public String getValueAt(byte[] inputBytes, int offset, int pos) throws IOException {
		
		int collectionSize = Storable.getInt(offset, inputBytes);
		if ( pos >= collectionSize) throw new IOException(
			"Maximum position in array is " + collectionSize + " and accessed " + pos );
		
		int elemSizeOffset = (offset + 4 + pos * 4);
		int elemStartOffset = Storable.getInt( elemSizeOffset, inputBytes);
		int elemEndOffset = Storable.getInt( elemSizeOffset + 4, inputBytes);
		//System.out.println(elemEndOffset + "-" + elemStartOffset);
		int elemLen = elemEndOffset - elemStartOffset;
		
		int headerOffset = (offset + 8 + collectionSize * 4);
		if ( 0 == elemLen) return "";
		byte[] aElem = new byte[elemLen];

		System.arraycopy(inputBytes, headerOffset + elemStartOffset, aElem, 0, elemLen);
		return new String(aElem);
	}

	@Override
	public int getEqualToIndex(byte[] inputData, String matchNo) throws IOException {
		return getEqualToIndex(inputData, 0, matchNo);
	}

	@Override
	public int getEqualToIndex(byte[] inputBytes, int offset, String matchVal) throws IOException {
		int collectionSize = Storable.getInt(offset, inputBytes);
		
		List<Integer> offsets = new ArrayList<Integer>();
		offset = offset + 4;
		
		for ( int i=0; i<collectionSize; i++) {
			int bytesLen = Storable.getInt( offset, inputBytes);
			offset = offset + 4;
			offsets.add(bytesLen);
		}
		
		int bodyLen = Storable.getInt(offset, inputBytes); // Find body bytes
		offsets.add(bodyLen);		
		offset = offset + 4;

		int headerOffset = offset;
		offsets.add( inputBytes.length - headerOffset);
		
		Integer thisElemOffset = -1;
		Integer nextElemOffset = -1;
		int elemOffset = -1;
		int elemLen = -1;
		boolean isSame = false;
		
		byte[] matchValB = matchVal.getBytes();
		for ( int i=0; i<collectionSize; i++) {
			thisElemOffset = offsets.get(i);
			nextElemOffset = offsets.get(i+1);
			elemOffset = (headerOffset + thisElemOffset);
			elemLen = nextElemOffset - thisElemOffset;
			isSame = ByteUtil.compareBytes(inputBytes, elemOffset, elemLen , matchValB);
			if ( isSame ) return i;
		}		
		return -1;
	}

	/**
	 * Find total entieis - 4 bytes
	 * Find the end bytes position to read
	 * Iterate to find String positions
	 * Read each string
	 */
	
	@Override
	public void getEqualToIndexes(byte[] inputData, String matchVal,
			Collection<Integer> matchings) throws IOException {
		
		if ( null == inputData) return;
		
		int collectionSize = Storable.getInt(0, inputData); // Find total entities - 4 bytes
		
		int headerLen = 4 + collectionSize * 4;
		
		if (inputData.length < headerLen) throw new IOException(
			"Corrupted bytes : collectionSize( " + collectionSize + "), header lengh=" + headerLen + 
					" , actual length = " + inputData.length);
		
		List<Integer> offsets = new ArrayList<Integer>(collectionSize);
		int offset = 4;
		for ( int i=0; i<collectionSize; i++) {
			int bytesLen = Storable.getInt( offset, inputData);
			offset = offset + 4;
			offsets.add(bytesLen);
		}
		int bodyLen = Storable.getInt(offset, inputData); // Find body bytes
		offsets.add(bodyLen);
		
		offset = offset + 4;
		if ( (offset + bodyLen)  < headerLen) throw new IOException(
				"Corrupted bytes : collectionSize( " + collectionSize + "), body Len=" + (offset + bodyLen) + 
				" , actual length = " + inputData.length);
		
		int headerOffset = offset;
		Integer thisElemOffset = -1;
		Integer nextElemOffset = -1;
		byte[] matchBytes = matchVal.getBytes(); 
		
		for ( int i=0; i<collectionSize; i++) {
			thisElemOffset = offsets.get(i);
			nextElemOffset = offsets.get(i+1);
			int elemOffset = (headerOffset + thisElemOffset);
			int elemLen = nextElemOffset - thisElemOffset;
			
			boolean isSame = ByteUtil.compareBytes(inputData, elemOffset, elemLen,  matchBytes);
			if ( isSame ) matchings.add(i);
		}		
	}

	@Override
	public void getGreaterThanIndexes(byte[] inputData, String matchingNo,
			Collection<Integer> matchingPos) throws IOException {
		throw new IOException("Not available");
	}

	@Override
	public void getGreaterThanEqualToIndexes(byte[] inputData,
			String matchingNo, Collection<Integer> matchingPos)
			throws IOException {
		throw new IOException("Not available");
	}

	@Override
	public void getLessThanIndexes(byte[] inputData, String matchingNo,
			Collection<Integer> matchingPos) throws IOException {
		throw new IOException("Not available");
	}

	@Override
	public void getLessThanEqualToIndexes(byte[] inputData, String matchingNo,
			Collection<Integer> matchingPos) throws IOException {
		throw new IOException("Not available");
	}

	@Override
	public void getRangeIndexes(byte[] inputData, String matchNoStart,
			String matchNoEnd, Collection<Integer> matchings)
			throws IOException {
		throw new IOException("Not available");
		
	}

	@Override
	public void getRangeIndexesInclusive(byte[] inputData, String matchNoStart,
			String matchNoEnd, Collection<Integer> matchings)
			throws IOException {
		throw new IOException("Not available");
		
	}

}