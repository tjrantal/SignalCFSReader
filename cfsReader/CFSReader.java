/*
CFS-file reader
Written by Timo Rantalainen 2012
Licensed with Gnu Public License 3.0 or above (http://www.gnu.org/copyleft/gpl.html)
File specifications taken from The CED filing system .pdf downloaded from (www.ced.co.uk/)
*/

package cfsReader;
import java.io.*;
import java.nio.ByteBuffer;	//Byte array to float
public class CFSReader{
	byte[] fileData;
	public CFSGenericHeader header;
	public DataSection[] datasection;
	public ChannelInformation[] channelInfo;
	public int currentOffset;
	/*Constants taken from cfc.h from www.ced.co.uk*/
	public static final int FILEVAR		=0;              // constants to indicate whether variable is 
	public static final int DSVAR		=1;                         // file or data section variable. 
	public static final int INT1		=0;                           // DATA VARIABLE STORAGE TYPES 
	public static final int WRD1		=1;
	public static final int INT2		=2;
	public static final int WRD2		=3;
	public static final int INT4		=4;
	public static final int RL4			=5;
	public static final int RL8			=6;
	public static final int LSTR		=7;
	public static final int SUBSIDIARY	=2;                            // Chan Data Storage types 
	public static final int MATRIX		=1;
	public static final int EQUALSPACED	=0;                            // Chan Data Storage types

	
	
	public CFSReader(String fileName){
		System.out.println("Reading "+fileName);
		fileData = readData(fileName);	//ReadData to memory
		
		/*General Header*/
		header = new CFSGenericHeader(fileData);
		currentOffset =  header.postOffset;
		header.print();
		
		/*Channel Information*/
		 channelInfo = new ChannelInformation[header.channelNo];
		for (int i = 0;i<header.channelNo;++i){
			channelInfo[i] = new ChannelInformation(fileData, currentOffset);
			currentOffset = channelInfo[i].postOffset;
		}
		
		/*File var info*/
		FileVariableInformation[] fileVarInfo = new FileVariableInformation[header.fileVarNo+1];
		for (int i = 0;i<header.fileVarNo+1;++i){
			fileVarInfo[i] = new FileVariableInformation(fileData, currentOffset);
			currentOffset = fileVarInfo[i].postOffset;
		}
				
		//Data section var info. N.B. the strucuture is similar to file var info
		FileVariableInformation[] dataSectionVarInfo = new FileVariableInformation[header.dataSectionVarNo+1];
		for (int i = 0;i<header.dataSectionVarNo+1;++i){
			dataSectionVarInfo[i] = new FileVariableInformation(fileData, currentOffset);
			currentOffset = dataSectionVarInfo[i].postOffset;
		}
		
		
		/*Skip file vars...*/
		currentOffset += fileVarInfo[header.fileVarNo].byteOffset;
		System.out.println("DS start "+currentOffset);
		
		 /*Get dataSection Pointers*/
		int[] dataSectionPointers = new int[header.numberOfDataSections];
		currentOffset = header.pointerTableOffset;
		for (int i = 0;i<header.numberOfDataSections;++i){//1;++i){//
			System.out.println("Reading pointer");
			dataSectionPointers[i] = readLong(fileData,currentOffset);
			System.out.println("Pointer "+i+":"+dataSectionPointers[i]);
			currentOffset +=4;
		}
		System.out.println("Got the DS pointers");
		/*Read Data Sections*/
		datasection = new DataSection[header.numberOfDataSections];
		for (int i = 0;i<dataSectionPointers.length;++i){//1;++i){//
			System.out.println("DS Header "+i);
			datasection[i] = new DataSection(fileData, dataSectionPointers[i],this);
		}
	}
	
	public byte[] readData(String fileName){
		File fileToRead = new File(fileName);
		long fileLength = fileToRead.length();
		byte[] fileData;
		try{
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileToRead));
			DataInputStream dataInputStream = new DataInputStream(inputStream);
			fileData = new byte[(int) fileLength];		//Allocate memory for reading the file into memory
			dataInputStream.read(fileData,0,(int) fileLength);		//Read the data to memory
			dataInputStream.close();	//Close the file after reading
			return fileData;
		}catch (Exception e) {
			System.out.println("Could not read input file "+e.toString());
		}
		return null;
	}
	
	public void readHeader(){
		
	}
	
	/*Functions for read specific data types from byte array...*/
	public static String readString(byte[] array,int offset){
		short stringLength = (short) (((int) array[offset]) & 0XFF);
		byte[] tempString = new byte[stringLength];
		for (int i = 0; i<stringLength; ++i){
			tempString[i] = array[offset+1+i];
		}
		return new String(tempString);
	}
	
	public static String readCharString(byte[] array,int offset,int length){
		byte[] tempString = new byte[length];
		for (int i = 0; i<length; ++i){
			tempString[i] = array[offset+i];
		}
		return new String(tempString);
	}
	
	public static float readFloat(byte[] array,int offset){
		byte[] returnValue = new byte[4];
		for (int i = 0; i<4; ++i){
			returnValue[3-i] = array[offset+i];
		}
		return ByteBuffer.wrap(returnValue).getFloat();
	}
	
	public static int readLong(byte[] array,int offset){
		int returnValue = 0;
		for (int i = 0; i<4; ++i){
			returnValue |= ((((int) array[offset+i]) & 0xFF)<<(8*i));
		}
		return returnValue;
	}
	
	public static short readShort(byte[] array,int offset){
		short returnValue = 0;
		for (int i = 0; i<2; ++i){
			returnValue |= (short) ((((int) array[offset+i]) & 0xFF)<<(8*i));
		}
		return returnValue;
	}
	
	public static byte readByte(byte[] array,int offset){
		byte returnValue = array[offset];
		return returnValue;
	}
	
	public static void main(String[] args){
		if (args.length < 1){
			System.out.println("Give file to read as an argument");
		}
		CFSReader reader = new CFSReader(args[0]);
		
	}
	
	/*Generic header*/
	class CFSGenericHeader{
		public String indetificationMarker;
		public String fileName;
		public int fileSize;
		public String timeCreated;
		public String dateCreated;
		public short channelNo;
		public short fileVarNo;
		public short dataSectionVarNo;
		public short fileHeaderLength;
		public short dataSectionHeaderLength;
		public int lastDataSectionHeaderOffset;
		public short numberOfDataSections;
		public short diskBlockSizeRounding;
		public String fileComment;
		public int pointerTableOffset;
		/*post read pointer*/
		public int postOffset;
		public CFSGenericHeader(byte[] data){
			//The CED filing system page 6...
			int[] offsets = {0x00,0x08,0x16,0x1a,0x22,0x2a,0x2c,0x2e,0x30,0x32,0x34,0x38,0x3a,0x3c,0x86};
			
			/*Read header info from memory*/
			int i = 0;
			indetificationMarker		= CFSReader.readCharString(data,offsets[i],8); ++i;
			fileName					= CFSReader.readString(data,offsets[i]); ++i;
			fileSize					= CFSReader.readLong(data,offsets[i]); ++i;
			timeCreated					= CFSReader.readCharString(data,offsets[i],8); ++i;
			dateCreated					= CFSReader.readCharString(data,offsets[i],8); ++i;
			channelNo					= CFSReader.readShort(data,offsets[i]); ++i;
			fileVarNo					= CFSReader.readShort(data,offsets[i]); ++i;
			dataSectionVarNo			= CFSReader.readShort(data,offsets[i]); ++i;
			fileHeaderLength			= CFSReader.readShort(data,offsets[i]); ++i;
			dataSectionHeaderLength		= CFSReader.readShort(data,offsets[i]); ++i;
			lastDataSectionHeaderOffset	= CFSReader.readLong(data,offsets[i]); ++i;
			numberOfDataSections		= CFSReader.readShort(data,offsets[i]); ++i;
			diskBlockSizeRounding		= CFSReader.readShort(data,offsets[i]); ++i;
			fileComment					= CFSReader.readString(data,offsets[i]); ++i;
			pointerTableOffset			= CFSReader.readLong(data,offsets[i]); ++i;
			postOffset = 0x8a+40;
		}
		

		
		public void print(){
			String[] descriptors = {"indetificationMarker","fileName","fileSize","timeCreated",
				"dateCreated","channelNo","fileVarNo","dataSectionVarNo","fileHeaderLength",
				"dataSectionHeaderLength","lastDataSectionHeaderOffset","numberOfDataSections",
				"diskBlockSizeRounding","fileComment","pointerTableOffset"};
			int i = 0;
			System.out.println(descriptors[i]+"\t"+indetificationMarker); ++i;
			System.out.println(descriptors[i]+"\t"+fileName); ++i;
			System.out.println(descriptors[i]+"\t"+fileSize); ++i;
			System.out.println(descriptors[i]+"\t"+timeCreated); ++i;
			System.out.println(descriptors[i]+"\t"+dateCreated); ++i;
			System.out.println(descriptors[i]+"\t"+channelNo); ++i;
			System.out.println(descriptors[i]+"\t"+fileVarNo); ++i;
			System.out.println(descriptors[i]+"\t"+dataSectionVarNo); ++i;
			System.out.println(descriptors[i]+"\t"+fileHeaderLength); ++i;
			System.out.println(descriptors[i]+"\t"+dataSectionHeaderLength); ++i;
			System.out.println(descriptors[i]+"\t"+lastDataSectionHeaderOffset); ++i;
			System.out.println(descriptors[i]+"\t"+numberOfDataSections); ++i;
			System.out.println(descriptors[i]+"\t"+diskBlockSizeRounding); ++i;
			System.out.println(descriptors[i]+"\t"+fileComment); ++i;
			System.out.println(descriptors[i]+"\t"+pointerTableOffset); ++i;
			System.out.println();
		}
	}
	
	class ChannelInformation{
		public String channelName;
		public String yUnits;
		public String xUnits;
		public byte dataType;
		public byte dataKind;
		public short byteSpaceBetweenElements;
		public short nextMarixOrMasterChannel;
		/*post read pointer*/
		public int postOffset;
		public ChannelInformation(byte[] data, int initOffset){
			int[] offsets = {0x00,0x16,0x20,0x2a,0x2b,0x2c,0x2e};
			
			int i = 0;
			channelName					= CFSReader.readString(data,offsets[i]+initOffset); ++i;
			yUnits						= CFSReader.readString(data,offsets[i]+initOffset); ++i;		
			xUnits						= CFSReader.readString(data,offsets[i]+initOffset); ++i;	
			dataType					= CFSReader.readByte(data,offsets[i]+initOffset); ++i;
			dataKind					= CFSReader.readByte(data,offsets[i]+initOffset); ++i;
			byteSpaceBetweenElements	= CFSReader.readShort(data,offsets[i]+initOffset); ++i;
			nextMarixOrMasterChannel	= CFSReader.readShort(data,offsets[i]+initOffset); ++i;
			postOffset = initOffset+offsets[offsets.length-1]+2;
			
		}
		
		public void print(){
			String[] descriptors = {"channelName","yUnits","xUnits","dataType",
				"dataKind","byteSpaceBetweenElements","nextMarixOrMasterChannel"};
			int i = 0;
			System.out.println(descriptors[i]+"\t"+channelName); ++i;
			System.out.println(descriptors[i]+"\t"+yUnits); ++i;
			System.out.println(descriptors[i]+"\t"+xUnits); ++i;
			System.out.println(descriptors[i]+"\t"+dataType); ++i;
			System.out.println(descriptors[i]+"\t"+dataKind); ++i;
			System.out.println(descriptors[i]+"\t"+byteSpaceBetweenElements); ++i;
			System.out.println(descriptors[i]+"\t"+nextMarixOrMasterChannel); ++i;
			System.out.println();
		}
		
	}
	
	class FileVariableInformation{
		public String description;
		public short dataType;
		public String unit;
		public short byteOffset;
		/*post read pointer*/
		public int postOffset;
		public FileVariableInformation(byte[] data, int initOffset){
			int[] offsets = {0x00,0x16,0x18,0x22};
			
			int i = 0;
			description					= CFSReader.readString(data,offsets[i]+initOffset); ++i;
			dataType	= CFSReader.readShort(data,offsets[i]+initOffset); ++i;
			unit						= CFSReader.readString(data,offsets[i]+initOffset); ++i;		
			byteOffset	= CFSReader.readShort(data,offsets[i]+initOffset); ++i;
			postOffset = initOffset+offsets[offsets.length-1]+2;
		}
		
		public void print(){
			String[] descriptors = {"description","dataType","unit","byteOffset"};
			int i = 0;
			System.out.println(descriptors[i]+"\t"+description); ++i;
			System.out.println(descriptors[i]+"\t"+dataType); ++i;
			System.out.println(descriptors[i]+"\t"+unit); ++i;
			System.out.println(descriptors[i]+"\t"+byteOffset); ++i;
			System.out.println();
		}
		
	}
	
	/* DATA SECTION CLASS with the subclasses*/
	/*Container for frame data, has sub classes for cotaining DS header*/
	public class DataSection{
		public GeneralDataSectionHeader generalDataSectionHeader;
		public DSChannelInformation[] dschannelInformation;
		public double[][] channelData;
		/*post read pointer*/
		public int currentOffset;
		public int dataSectionStartOffset;
		public DataSection(byte[] data, int initOffset,CFSReader cfsReader){
			dataSectionStartOffset = initOffset;
			/*Read general datasection header*/
			generalDataSectionHeader = new GeneralDataSectionHeader(data,initOffset);
			generalDataSectionHeader.print();
			
			currentOffset =  generalDataSectionHeader.postOffset;
			/*Read channel informations*/
			dschannelInformation = new DSChannelInformation[cfsReader.header.channelNo];
			for (int i = 0;i<cfsReader.header.channelNo;++i){
				dschannelInformation[i] = new DSChannelInformation(data, currentOffset);
				currentOffset = dschannelInformation[i].postOffset;
			}
			/*Read Data*/
			channelData = new double[cfsReader.header.channelNo][];
			for (int i = 0;i<cfsReader.header.channelNo;++i){
				channelData[i] = new double[dschannelInformation[i].dataPoints];
				for (int j = 0; j<dschannelInformation[i].dataPoints;++j){
					channelData[i][j] = ((double) (CFSReader.readShort(data,generalDataSectionHeader.pointerToChannelData+dschannelInformation[i].offsetInDataSectionToFirstByte+j*channelInfo[i].byteSpaceBetweenElements)))*(double)dschannelInformation[i].yScale;
				}
			}
			
		}
	
		public class GeneralDataSectionHeader{
			public int pointerToPreviousDSHeader;
			public int pointerToChannelData;
			public int sizeOfChannelDataArea;
			public short flagsForMarkingDataSections;
			/*post read pointer*/
			public int postOffset;
			public GeneralDataSectionHeader(byte[] data, int initOffset){
				int[] offsets = {0x00,0x04,0x08,0x0c,0x0e};
				
				int i = 0;
				pointerToPreviousDSHeader	= CFSReader.readLong(data,offsets[i]+initOffset); ++i;
				pointerToChannelData	= CFSReader.readLong(data,offsets[i]+initOffset); ++i;
				sizeOfChannelDataArea			= CFSReader.readLong(data,offsets[i]+initOffset); ++i;
				flagsForMarkingDataSections	= CFSReader.readShort(data,offsets[i]+initOffset); ++i;
				postOffset = initOffset+offsets[offsets.length-1]+16;
			}
			public void print(){
				String[] descriptors = {"pointerToPreviousDSHeader","pointerToChannelData","sizeOfChannelDataArea"};
				int i = 0;
				System.out.println(descriptors[i]+"\t"+pointerToPreviousDSHeader); ++i;
				System.out.println(descriptors[i]+"\t"+pointerToChannelData); ++i;
				System.out.println(descriptors[i]+"\t"+sizeOfChannelDataArea); ++i;
				System.out.println();
			}
		}
		
		public class DSChannelInformation{
			public int offsetInDataSectionToFirstByte;
			public int dataPoints;
			public float yScale;
			public float yOffset;
			public float xIncrement;
			public float xOffset;
			
			
			/*post read pointer*/
			public int postOffset;
			public DSChannelInformation(byte[] data, int initOffset){
				int[] offsets = {0x00,0x04,0x08,0x0c,0x10,0x14};
				
				int i = 0;
				offsetInDataSectionToFirstByte	= CFSReader.readLong(data,offsets[i]+initOffset); ++i;
				dataPoints	= CFSReader.readLong(data,offsets[i]+initOffset); ++i;
				yScale	= CFSReader.readFloat(data,offsets[i]+initOffset); ++i;
				yOffset	= CFSReader.readFloat(data,offsets[i]+initOffset); ++i;
				xIncrement	= CFSReader.readFloat(data,offsets[i]+initOffset); ++i;
				xOffset	= CFSReader.readFloat(data,offsets[i]+initOffset); ++i;
				postOffset = initOffset+offsets[offsets.length-1]+4;
			}
		}
	}

}