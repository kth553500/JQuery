<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    import="java.sql.*,kth.board.*"%>

<%
	Connection con = null;
	PreparedStatement pstmt =null;
	ResultSet rs = null;
	DBConnectionMgr pool = null;
	String sql="";
	
	try{
		pool = DBConnectionMgr.getInstance();
		con=pool.getConnection();
		System.out.println("con=>" +con);
		sql="select*from board order by num asc";
		pstmt=con.prepareStatement(sql);
		rs=pstmt.executeQuery();
		while(rs.next()){
			//[{num:1,writer:'홍길동',~},{},{},,,]
			int num = rs.getInt("num");
			String writer=rs.getString("writer");
			String subject=rs.getString("subject");
			String content=rs.getString("content");
			if(rs.getRow() > 1){ //rs.getRow()->레코드 갯수얻어옴
				out.print(",");
			} %>
			{
			 "num" : <%=num %>,<br>
			 "writer":<%=writer %>,<br>
			 "subject":<%=subject %>,<br>
			 "content":<%=content %>,<br>
			}
<%		
		}
	}catch(Exception e){
		out.println("getBoardJson.jsp에 에러유발=>"+e);
	} finally{
		pool.freeConnection(con,pstmt,rs);
	}
%>

]
