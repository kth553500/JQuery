package kth.board;
import java.sql.*;
import java.util.*; //ArrayList,HashTable..

//DBConnectionMgr(DB관리(10개)),
//BoardDTO(데이터를 입력받아서 저장, 메서드 호출할때 (매개변수,반환형 으로 사용하기위해서 필요))
public class BoardDAO {
	private DBConnectionMgr pool = null; //1.객체 생성할 멤버변수선언
	//공통
	private Connection con = null;
	private PreparedStatement pstmt = null;
	private ResultSet rs = null; //select
	String sql = "";//실행시킬 sql구문
	
	//2.생성자를 통해서 연결**=>의존관계
	public BoardDAO() {
		try {
			pool = DBConnectionMgr.getInstance();//DBConnectionMgr pool = new DBConnectionMgr();
			//다른메서드에서 pool= new DBConnectionMgr(); <--이렇게 호출 안해도 됨. DAO클래스호출할때마다 자동적호출
			System.out.println("pool=>" + pool);
		} catch (Exception e) {
			System.out.println("DB접속오류=>" +e);
		}
	}
	//3. 
	//(1)메서드 작성(페이징 처리를 위한 메서드)=>총레코드수(=총게시물수=총회원수)
	//select count(*) from board; =>jspMember : select count(*) from member;
	public  int getArticleCount() {//getMemberCount()
		int x = 0; //총레코드수
		try {
			con = pool.getConnection();
			System.out.println("con->" + con);
			sql = "select count(*) from board";
			pstmt=con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if(rs.next()) {
				x = rs.getInt(1);
			}
			
		}catch(Exception e) { 
			System.out.println("getArticleCount()에러유발->" + e);
		}finally {
			pool.freeConnection(con, pstmt, rs);
		}
		return x;
	}
	
	//(2)글목록보기에 대한 메서드구현->레코드 한개이상->한페이지당 10개씩 담기
	//1) 레코드의 시작번호 2) 불러올 레코드의 갯수(ex 10 , 20 , 30....)
	public List<BoardDTO> getArticles(int start, int end){
		List<BoardDTO> articleList = null;
		
		try {
			con = pool.getConnection();
			/*
			 * 그룹번호가 가장 최신의 글을 중심으로 정렬하되, 만약에 level이 같은경우
			 * step값으로 오름차순을 통해서 몇번째 레코드번호를 기준해서 몇개까지 정렬할것인가를 지정해주는 SQL구문
			 */
			sql = "select * from board order by ref desc, re_step limit ?,?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, start-1); //mysql은 레코드순번이 내부적으로 0시작
			pstmt.setInt(2, end); //불러와서 담을 갯수(ex 10)
			rs = pstmt.executeQuery();
			if(rs.next()) { //보여주는 결과가 있다면
				articleList = new ArrayList(end); //10->end갯수만큼 데이터담을 공간생성
				do {
					BoardDTO article = makeArticleFromResult();
					/*
					BoardDTO article = new BoardDTO(); //new MemberDTO();
					article.setNum(rs.getInt("num"));
					article.setWriter(rs.getString("writer")); //필드오타 : 부적합한열입니다. 
					article.setEmail(rs.getString("email"));
					article.setSubject(rs.getString("subject"));
					article.setPasswd(rs.getString("passwd"));
					article.setReg_date(rs.getTimestamp("reg_date"));//작성날짜
					article.setReadcount(rs.getInt("readcount"));//default 0
					article.setRef(rs.getInt("ref")); //그룹번호->신규글,답변글 묶어줌
					article.setRe_step(rs.getInt("re_step"));
					article.setRe_level(rs.getInt("re_level"));
					article.setContent(rs.getString("content"));
					article.setIp(rs.getString("ip"));
					*/
					
					//추가
					articleList.add(article); //생략하면 데이터가 저장X->for문시 에러유발
				}while (rs.next());
			}
		} catch (Exception e) {
			System.out.println("getArticles에러유발->" + e);
		}finally {
			pool.freeConnection(con,pstmt,rs);
		}
		return articleList;
	}
	
	
	//게시판의 글쓰기 및 답변달기
	//insert into board values(?,?...)
	public void insertArticle(BoardDTO article) {//~(MemberDTO)
		//1.article->신규글인지 답변글인지(기존 게시물번호)인지 확인
		int num = article.getNum(); //0 신규글<->0이 아닌경우(양수)
		int ref=article.getRef();
		int re_step=article.getRe_step();
		int re_level = article.getRe_level();
		
		int number=0; //데이터를 저장하기위한 게시물 번호(=New)
		System.out.println("insertArticle 메서드의 내부 num=>" + num);
		System.out.println("ref=>" +ref + "re_step->" + re_step + "re_level=>" + re_level);
		
		try {
			con=pool.getConnection();
			sql = "select max(num) from board";
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			
			if(rs.next()) {//데이터가 있다면
				number = rs.getInt(1)+1; //최대값+1
			}else {
				number = 1; //테이블의 한개의 데이터가 없다면 최초 1부여
			}
			
			//답변글이라면
			if(num!=0) {
				//같은 그룹번호를 가지고 있으면서 나보다 step값이 큰 게시물을 찾아서 step하나 증가
				sql = "update board set re_step=re_step+1 where ref=? and re_step>?"; //기존있었던 답변글
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, ref);
				pstmt.setInt(2, re_step);
				int update = pstmt.executeUpdate();
				//1.성공(댓글만들어지는 위치) 0 실패
				System.out.println("댓글 수정유무(update)=>" + update);
				//답변글
				re_step = re_step+1; //신규답변글 + 1
				re_level = re_level+1;
				
			} else { //신규글이라면 num = 0
				ref = number; //num=1 ref=1,2,3,4...
				re_step = 0; //답변순서 X
				re_level = 0; //답변자체 X
			}
			//12개-> num,reg_date,readcount(생략)->default
			//작성날짜 = > sysdate->오라클, now() ->mysql
			sql = "insert into board(writer,email,subject,passwd,reg_date,";
			sql+= "ref,re_step,re_level,content,ip) values(?,?,?,?,?,?,?,?,?,?)";
//			sql+= "ref,re_step,re_level,content,ip) values(?,?,?,?,now(),?,?,?,?,?)";
			pstmt= con.prepareStatement(sql);
			pstmt.setString(1, article.getWriter());
			pstmt.setString(2, article.getEmail());
			pstmt.setString(3, article.getSubject());
			pstmt.setString(4, article.getPasswd());
			pstmt.setTimestamp(5, article.getReg_date());// now()
			//----ref,re_step,re_level (계산된 값이 저장)
			pstmt.setInt(6, ref ); //최대값+1
			pstmt.setInt(7,re_step);//0
			pstmt.setInt(8,re_level);//0
			//--------------------------------------------
			pstmt.setString(9, article.getContent());
			pstmt.setString(10, article.getIp()); //request.getRemoteAddr()
			int insert = pstmt.executeUpdate();
			System.out.println("게시판의 글쓰기성공유무(insert)=>" + insert); //1-> 성공 0->실패
			
		} catch (Exception e) {
			System.out.println("insertArticle()에러유발=>" + e);
		} finally {
			pool.freeConnection(con, pstmt, rs);
		}
	}
	
	//글상세보기->소스코드를 적게 사용하는 방법을 선택
	//update board set readcount=readcount+1 where num=3;//조회수 증가
	//select * from board where num=3;

	public BoardDTO getArticle(int num) {
			BoardDTO article = null;
		try {
			con = pool.getConnection();
			sql = "update board set readcount=readcount+1 where num=?"; //조회수 증가
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			int update = pstmt.executeUpdate();
			System.out.println("조회수 증가유무(update)=>" +update);
			
			sql = "select * from board where num=?"; 
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs = pstmt.executeQuery();
			if(rs.next()) {
				article = makeArticleFromResult();
				/*
				article = new BoardDTO();
				article.setNum(rs.getInt("num"));
				article.setWriter(rs.getString("writer")); 
				article.setEmail(rs.getString("email"));
				article.setSubject(rs.getString("subject"));
				article.setPasswd(rs.getString("passwd"));
				article.setReg_date(rs.getTimestamp("reg_date"));//작성날짜
				article.setReadcount(rs.getInt("readcount"));//default 0
				article.setRef(rs.getInt("ref")); //그룹번호->신규글,답변글 묶어줌
				article.setRe_step(rs.getInt("re_step"));
				article.setRe_level(rs.getInt("re_level"));
				article.setContent(rs.getString("content"));
				article.setIp(rs.getString("ip"));	
				*/
			}
		} catch (Exception e) {
			System.out.println("getArticle에러유발->" + e);
		}finally {
			pool.freeConnection(con,pstmt,rs);
		}
		return article; 
	}
	
	
	//접근지정자가 private가 되는 경우 =>외부에서 호출X 내부 클래스사용O //코드 중복성 감소*****
	private BoardDTO makeArticleFromResult() throws Exception{
	
		BoardDTO article = new BoardDTO();
		article.setNum(rs.getInt("num"));
		article.setWriter(rs.getString("writer")); 
		article.setEmail(rs.getString("email"));
		article.setSubject(rs.getString("subject"));
		article.setPasswd(rs.getString("passwd"));
		article.setReg_date(rs.getTimestamp("reg_date"));//작성날짜
		article.setReadcount(rs.getInt("readcount"));//default 0
		article.setRef(rs.getInt("ref")); //그룹번호->신규글,답변글 묶어줌
		article.setRe_step(rs.getInt("re_step"));
		article.setRe_level(rs.getInt("re_level"));
		article.setContent(rs.getString("content"));
		article.setIp(rs.getString("ip"));	
		
		return article;
	}
	
	
	//select * from board where num=3;
	//글수정
	//1)수정할 데이터를 찾을 메서드 필요
	public BoardDTO updateGetArticle(int num) {
		BoardDTO article = null;
		try {
			con = pool.getConnection();		
			sql = "select * from board where num=?"; 
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs = pstmt.executeQuery();
			if(rs.next()) {
				article = makeArticleFromResult();
			}
		} catch (Exception e) {
			System.out.println("updateGetArticle()메서드에러유발->" + e);
		}finally {
			pool.freeConnection(con,pstmt,rs);
		}
		return article; //updateForm.jsp에서 반환받기
	}
	
	// 2)수정시켜주는 메서드 작성 -> 본인확인 // 회원탈퇴(암호를 비교 => 탈퇴)
	public int updateArticle(BoardDTO article) {
		String dbpasswd = ""; // DB상에서 찾은 암호를 저장
		int x = -1; // 게시물의 수정유무

		try {
			con = pool.getConnection();
			sql = "select passwd from board where num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, article.getNum());
			rs = pstmt.executeQuery();

			if (rs.next()) {// 데이터가 있다면
				dbpasswd = rs.getString("passwd");
				System.out.println("dbpasswd=>" + dbpasswd);
				
				if (dbpasswd.equals(article.getPasswd())) {
					sql = "update board set writer=?, email=?, subject=?,";
					sql += " passwd=?, content=? where num=?";
					pstmt = con.prepareStatement(sql);
					pstmt.setString(1, article.getWriter());
					pstmt.setString(2, article.getEmail());
					pstmt.setString(3, article.getSubject());
					pstmt.setString(4, article.getPasswd());
					pstmt.setString(5, article.getContent());
					pstmt.setInt(6, article.getNum());
					int update = pstmt.executeUpdate();
					System.out.println("게시판의 글수정유무(update)=>" + update); // 1-> 성공 0->실패
					x = 1;// 성공
				} else {
					x = 0;// 수정실패
				}
			} else {//암호가 존재X
				x=-1;
			}
		} catch (Exception e) {
			System.out.println("updateArticle()에러유발=>" + e);
		} finally {
			pool.freeConnection(con, pstmt, rs);
		}
		return x;
	}
	
	//삭제시켜주는 메서드
	//select passwd from board where num=? //hidden
	//delete from board where num=?
	public int deleteArticle(int num, String passwd) {
		String dbpasswd = ""; // DB상에서 찾은 암호를 저장
		int x = -1; // 게시물의 삭제 유무

		try {
			con = pool.getConnection();
			sql = "select passwd from board where num=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, num);
			rs = pstmt.executeQuery();

			if (rs.next()) {// 데이터가 있다면
				dbpasswd = rs.getString("passwd");
				System.out.println("dbpasswd=>" + dbpasswd);
				/*
				 * 암호가 틀리다고 하는 경우-> num가 제대로 전달?
				 * 입력받은 암호를 제대로 검증을 못하는 경우
				 */
				if (dbpasswd.equals(passwd)){
					sql = "delete from board where num=?";
					pstmt = con.prepareStatement(sql);
					pstmt.setInt(1,num);
					int delete = pstmt.executeUpdate();
					System.out.println("게시판의 글삭제유무(delete)=>" + delete); // 1-> 성공 0->실패
					x = 1;// 성공
				} else {
					x = 0;// 삭제실패
				}
			} else {//데이터없음
				x=-1;
			}
		} catch (Exception e) {
			System.out.println("deleteArticle()에러유발=>" + e);
		} finally {
			pool.freeConnection(con, pstmt, rs);
		}
		return x;
	}
	
	
	//select writer from board where writer like '%?%';
	public List<String> getArticleId(String name){
		List<String> nameList = new ArrayList();
		try {
			con=pool.getConnection();
			sql="select writer from board where writer like '%" + name + "%'";
			pstmt=con.prepareStatement(sql);
			rs=pstmt.executeQuery();
			while(rs.next()) {
				String writer= rs.getString("writer");
				nameList.add(writer);
			}
		} catch (Exception e) {
			System.out.println("getArticleId()에러유발->" + e);
		} finally {
			pool.freeConnection(con, pstmt, rs);
		}
		return nameList;
	}

}
