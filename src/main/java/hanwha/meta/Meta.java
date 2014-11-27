package hanwha.meta;

import static hanwha.util.Extend.extend;
import static org.junit.Assert.assertEquals;
import hanwha.util.IntList;
import hanwha.util.IntSet;
import hanwha.util.PairSet;
import hanwha.util.Set;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Meta {
	private static final String  DATA_DIR = "."; 
	private static final File   DATA_FILE = new File(DATA_DIR, 
			                                    Meta.class.getName() + ".data");
	private static boolean     refreshing = false;
	private static AtomicInteger useCount = new AtomicInteger();

	static {
		try {
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static   int[]    intSet; // 펀드코드 집합
	private static  Data[]   dataSet; // 데이터 집합
	private static short[][] pairSet; // 정수 짝 집합
	private static short[]  dataList; // 데이터 열
	private static short[] data1List; 
	
	/**
	 * 보험료/준비금/사업비 찾기 키 구성 정보를 갱신한다.
	 * 데이터 파일이 없으면 데이터베이스에서 다운로드하여 새로 만든다.
	 * @throws Exception 데이터 읽기 에러 또는 download() 에러.
	 */
	public static synchronized void refresh() throws Exception {
		final String DATA_PATH = DATA_FILE.getCanonicalPath();
		time(null);
		if (DATA_FILE.length() < 1) {
			download(DATA_FILE);
			if (DATA_FILE.length() < 1) {
				throw new Exception(DATA_PATH + " 파일을 만들 수 없습니다.");
			}
		}
        try (ObjectInputStream in = new ObjectInputStream(
                                    new BufferedInputStream(
                                    new FileInputStream(DATA_FILE)))) {
    		time(DATA_PATH);
    		refreshing = true;
    		while (0 < useCount.get()) {
    			Thread.sleep(1);
    		}
    		intSet     =     (int[]) in.readObject(); // 펀드코드 집합
    		dataSet    =    (Data[]) in.readObject(); // 데이터 집합
    		pairSet    = (short[][]) in.readObject(); // 정수 짝 집합
    		dataList   =   (short[]) in.readObject(); // 데이터 열
    		data1List  =   (short[]) in.readObject(); 
    		refreshing = false;
    		time("Read");
        }
		System.out.format("%d KBytes\n", (DATA_FILE.length() + 1023) / 1024);
	}

	/**
	 * SQL을 실행하여 얻은 데이터로 메모리 이미지를 만들고 파일에 기록한다.
	 * @param file 메모리 이미지 파일
	 * @throws Exception 테이블 읽기 오류, 데이터 배열 원소 크기 오류.
	 */
	public static void download(File file) throws Exception {
		IntSet     intSet0 = new  IntSet(3000);
		Set<Data> dataSet0 = new   Set<>(300);
		PairSet   pairSet0 = new PairSet(10000);
		IntList  dataList0 = new IntList(10000, 2000);
		IntList data1List0 = new IntList(10000, 3000);
		ImCvr       imCvr0 = new   ImCvr(3000);

		time(null);
		List<Row> rows = Dao.getRowList();    // 키 구성 정보를 모두 읽는다
		time("SQL");

		int rowCount = rows.size();           // 키 구성 정보 수
		rows.add(new Row());                  // 키 구성 정보의 끝을 표시한다

		for (int i = 0; i < rowCount; i++) {
			Row thisRow = rows.get(i);
			Row nextRow = rows.get(i + 1);
			dataList0.append(dataSet0.find(thisRow.data)); // 데이터

			if (!thisRow.sameImcd(nextRow)) {
				int cdNddt = intSet0.find(thisRow.imCd); // 펀드코드
				imCvr0.append(cdNddt);
				int   data = dataList0.find(pairSet0);     // 데이터 리스트
				data1List0.append(pairSet0.find(cdNddt, data));
				data1List0.find();

			}
		}
		time("Index");
        try (ObjectOutputStream out = new ObjectOutputStream(
        		                      new BufferedOutputStream(
        		                      new FileOutputStream(file)))) {
        	out.writeObject(intSet0.copy());
        	out.writeObject(dataSet0.copy());
        	out.writeObject(pairSet0.copyToShorts());
        	out.writeObject(dataList0.copyToShorts());
        	out.writeObject(data1List0.copyToShorts());
        	out.writeObject(imCvr0.copyIm());
        }
		time("Write");

		System.out.format("\n%d rows --> %d KBytes (%s)\n" +
		           "intSet[%d] pairSet[2][%d] dataSet[%d]\n" +
		           "dataList[%d](%d) data1List[%d](%d)",
		                        rowCount, (file.length() + 1023) / 1024,
		                        file.getAbsolutePath(),
		                  intSet0.size(), pairSet0.size(), dataSet0.size(),
		                dataList0.size(),  dataList0.indexSize(),
		               data1List0.size(), data1List0.indexSize());
	}

	/**
	 * 경과 시간을 출력한다.
	 * @param title 제목. null이면 경과 시간 측정 시작.
	 */
	private static void time(String title) {
		nano = System.nanoTime();
		if (title != null) {
			System.out.format("%s(%.3f초) ", title, (nano - nano0)/1000000000.);
		}
		nano0 = nano;
	}

	private static long nano0, nano;  // 나노 초 단위 경과 구간: 시작, 끝

	/**
	 * 펀드 데이터 열 -- (펀드 코드, 담보 코드 인덱스 열, 담보 데이터 열)의 열
	 */
	private static class ImCvr {
		private int[]      im;  // 펀드 코드 배열
	//	private short[][] cvr;  // 담보(코드 인덱스 열 시작, 끝, 데이터 열 시작) 배열
		private int      size;  // 펀드 수

		/**
		 * @param initialCapacity 처음 배열 용량(= 펀드 수 최대값)
		 */
		ImCvr(int initialCapacity) {
			im  = new int[initialCapacity];
	//		cvr = new short[3][initialCapacity];
		}

//		/**
//		 * 테이블에 한 펀드의 데이터를 넣는다
//		 * @param imCd    펀드 코드
//		 * @param cvrCds  담보 코드 인덱스 배열의 {시작 인덱스, 끝 인덱스}
//		 * @param cvrData 담보 데이터 배열의 {시작 인덱스, 끝 인덱스}
//		 */
//		void append(int imCd, int[] cvrCds, int[] cvrData) throws Exception {
//			int a = cvrCds[0], b = cvrCds[1], c = cvrData[0];
//			if (a < Short.MIN_VALUE || Short.MAX_VALUE < a ||
//			    b < Short.MIN_VALUE || Short.MAX_VALUE < b ||
//			    c < Short.MIN_VALUE || Short.MAX_VALUE < c) {
//				throw new Exception("short형으로 받을 수 없습니다.");
//			}
//			if (im.length <= size) {
//				im  = Arrays.copyOf(im, extend(size));
//				cvr = copyCvr(extend(size));
//			}
//			im[size]     = imCd;       // 펀드 코드
//			cvr[0][size] = (short) a;  // 담보 코드 인덱스 배열 시작 인덱스
//			cvr[1][size] = (short) b;  // 담보 코드 인덱스 배열 끝 인덱스
//			cvr[2][size] = (short) c;  // 담보 데이터 배열 시작 인덱스
//			size++;
//		}
		
		/**
		 * 펀드명만 삽입 krh
		 * @param imCd 펀드코드
		 * 
		 * @return
		 */
		void append(int imCd) throws Exception{
			
			if (im.length <= size) {
				im  = Arrays.copyOf(im, extend(size));
			}
			im[size]     = imCd;       // 펀드 코드
			size++;
		}

		int[] copyIm() {
			return Arrays.copyOf(im, size);
		}


//
//		private short[][] copyCvr(int newSize) {
//			return new short[][] { 
//				Arrays.copyOf(cvr[0], newSize),
//				Arrays.copyOf(cvr[1], newSize),
//				Arrays.copyOf(cvr[2], newSize)
//			};
//		}
	}


	/**
	 * 보험료/준비금/사업비 찾기 키 배열을 만든다.
	 *
	 * @param 펀드코드           펀드코드 글자열
	 * @param 펀드명           펀드명 글자열
	 * @param 펀드정보           Map(펀드정보 이름, 펀드정보 값)
	 * @return 4개 키 배열
	 */
	public static Object[] getKeys(int 펀드코드,
	                               String 펀드명,
	                               Map<String, Object> 펀드정보) {

		int imCd = 펀드코드;

		Object[] keys = Data.DEFAULT_KEYS.clone();

		useCount.incrementAndGet();
		if (refreshing) {
			useCount.decrementAndGet();
			while (refreshing) {
				try { Thread.sleep(1); } catch (Exception e) {}
			}
			useCount.incrementAndGet();
		}

		do {
			int i=0;
			int iEnd=data1List.length-1;
			
			while (i < iEnd) {
				int  data1 = data1List[i++];       // (펀드코드, 데이터) 짝
				int cdd=intSet[pairSet[0][data1]];
				if(cdd == imCd){
					dataSet[dataList[pairSet[1][data1]]].getKey(keys, 펀드정보);
					break;
				}				
			}
		} while (false);

		useCount.decrementAndGet();
		return keys;
	}

	/**
	 * 테이블에서 읽은 행과 맞추어 데이터를 검사하거나, 데이터를 출력한다.
	 *
	 * @param from  출력 시작 펀드 인덱스  -- 0, 1, 2, ...
	 * @param count 출력 펀드 수 -- 0이면 데이터를 모두 검사한다.
	 * @param file  출력 파일 이름 -- null이거나 잘못된 이름이면 표준 출력으로 출력한다.
	 * @throws Exception 데이터베이스 테이블 읽기 오류. 
	 */
	public static void testOrPrint(int from, int count, String file)
		                    throws Exception {
		boolean testing = count == 0;
		int          to = Math.min(from + count, data1List.length);
		PrintStream out = System.out;
		List<Row>  rows = null;

		if (testing) {
			from = 0;                 // 처음부터
			to   = data1List.length;         // 끝까지
			rows = Dao.getRowList();  // 테이블에서 읽은 데이터 행 리스트
		} else {
			if (file != null) {
				try {
					out = new PrintStream(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		int r = 0;                                  // 행 인덱스
		for (int m = from; m < to; m++) {           // 펀드 인덱스
					
				int  data1 = data1List[m];       // (펀드코드, 데이터) 짝
				int cdd=intSet[pairSet[0][data1]];

					
					
						if (testing) {
							String msg = "행 인덱스 = " + r;
							Row    row = rows.get(r++);
							
							assertEquals(msg, row.imCd, cdd);
							assertEquals(msg, row.data, dataSet[dataList[pairSet[0][data1]]]);
						} else {
							out.format("%s %s \n", cdd,dataSet[dataList[pairSet[0][data1]]] );
						}
		}

		if (testing) {
			assertEquals("행 수", rows.size(), r);  // 행 수가 맞는지 검사한다
		} else {
			if (out != System.out) {
				out.close();
			}
		}
	}

	private Meta() {}
}
