package hanwha.meta;

/**
 * 종목코드, 담보코드, 구분_종료일과 그것에 해당하는 데이터
 */
class Row {
	int  imCd, cvrCd, cdNddt;  // 종목코드, 담보코드, 구분_종료일
	Data data;                 // 데이터

	// 테이블구분과 종료일이 같은가?
	boolean sameCdNddt(Row o) {
		return cdNddt == o.cdNddt && cvrCd == o.cvrCd && imCd == o.imCd;
	}

	// 담보코드가 같은가?
	boolean sameCvrcd(Row o) {
		return cvrCd == o.cvrCd && imCd == o.imCd;
	}

	// 종목코드가 같은가?
	boolean sameImcd(Row o) {
		return imCd == o.imCd;
	}
}
