package hanwha.meta;

import java.io.Serializable;
import java.util.Map;

/**
 * 키 구성 메타 데이터
 */
class Data implements Comparable<Data>, Serializable {
	private static final long serialVersionUID = -2650552921340915444L;
	private int    keyIndex;  // 키 인덱스
	private String keyItnm;   // 키값항목명
	private String cndItnm;   // 조건항목명
	private String    cndOp;     // 비교연산자
	private int    cndValue;  // 조건값

	Data(int kIndex, String kItnm, String cItnm, String cOp, int cValue) {
		keyIndex = kIndex;
		keyItnm  = kItnm;
		cndItnm  = cItnm == null? "": cItnm;
		cndOp    = cOp;
		cndValue = cValue;
	}

	@Override
	public int compareTo(Data o) {
//	public int compareTo(Data o) {
		int diff;
		if (0 != (diff = keyIndex - o.keyIndex))        return diff;
		if (0 != (diff = keyItnm.compareTo(o.keyItnm))) return diff;
		if (0 != (diff = cndItnm.compareTo(o.cndItnm))) return diff;
//		if (0 != (diff = cndOp - o.cndOp))              return diff;
		return cndValue - o.cndValue;
	}


	@Override
	public boolean equals(Object o) {
		return compareTo((Data) o) == 0;
	}

	@Override
	public String toString() {
		String s = "(" + keyIndex + " " + keyItnm + ")";
//		if (0 < cndOp) {
//			s += "(" + cndItnm + " " + cndOp + " " + cndValue + ")";
//		}
		return s;
	}

	static final Object[] DEFAULT_KEYS = {
		"*","*","*", "*"};         

	private static final boolean[] IS_KEY_INT = { 
		false, false, false, false};

	// 조건을 검사하여 키 배열에 값을 넣는다 
	void getKey(Object[] keys, Map<String, Object> 펀드정보) {
		boolean isConditionMet = true;
//		if (0 < cndOp) {
//			Object o = 펀드정보.get(cndItnm);
//			int value;
//			if (o == null) {
//				value = 0;
//			} else if (o instanceof String) {
//				value = Integer.parseInt((String) o);
//			} else {
//				value = (Integer) o;
//			}
//			switch (cndOp) {
//			case 1: isConditionMet = value == cndValue; break;
//			case 2: isConditionMet = value != cndValue; break;
//			case 3: isConditionMet = value >  cndValue; break;
//			case 4: isConditionMet = value <  cndValue; break;
//			case 5: isConditionMet = value >= cndValue; break;
//			case 6: isConditionMet = value <= cndValue; break;
//			}
//		}
		if (isConditionMet) {
			Object value = 펀드정보.get(keyItnm);
			if (value != null) {
				if (IS_KEY_INT[keyIndex]) {
					if (value instanceof String) {
						value = Integer.parseInt((String) value);
					}
				} else {
					if (value instanceof Integer) {
						value = String.valueOf(value);
					}
				}
				keys[keyIndex] = value;
			}
		}
	}
}
