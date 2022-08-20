<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<script type="text/javascript" src="http://code.jquery.com/jquery-3.3.1.min.js">

</script>
<title>08.inputaatr.html</title>
<script>
$(function(){

	var map = {};
	var array = [{}, {}, {}];
	var data = [{
		id: "1",
		idx: 1
	}, {
		id: "2",
		idx: 2
	}, {
		id: "3",
		idx: 3
	}];
	
	var html1 = "";
	for(var i = 0; i < 3; i++) {
		if(data[i]["id"] != "2") {
			html1 += "<tr>";
			html1 += "<td style='color: red;'>1</td>";
			html1 += "<td>2</td>";
			html1 += "<td><a href='/main.do?pageNum=" + (data[i]["idx"] + 1) + "'>3</a></td>";
			html1 += "<td>5</td>";
			html1 += "<td>6</td>";
			html1 += "<td>27</td>";
			html1 += "</tr>";
		}
	}
	
	$("#tableMain").append(html1);
//jQuery를통해서 받아온 데이터를 반복문을 통해 화면에 출력해줄수 잇다
	

//전체적인 흐름
// jsp(html) ->//추가 jquery(script) -> ajax //->server servlet(controller)
//                                           -> db connection -> result(db) -> ajax -> jquery
// EX) insert
// jsp 회원가입-> jquery(회원id체크,우편번호체크) ->ajax(jquery문장을 서블릿(컨트롤러를 호출하는 역할) 
//       ->servlet(dbconection)(DAO클래스의 insert메서드호출) ->result(제대로 값 전달시 check=1반환
//		->ajax(check에담긴값을 받아옴) ->jquery를 통해 jsp화면에 뿌려줌(회원가입 되셨습니다.)



	});
	
})
</script>
</head>
<body>
    <form id="fomMain">
    	<input type="text">
    	<input type="text">
    	<input type="text">
    </form>
	<button id="btnMain">전송</button>
	<table id="tableMain" border="1" width="700" cellpadding="0" cellspacing="0" align="center"> 
	    <tr height="30" bgcolor="#b0e0e6"> 
	      <td align="center"  width="50"  >번 호</td> 
	      <td align="center"  width="250" >제   목</td> 
	      <td align="center"  width="100" >작성자</td>
	      <td align="center"  width="150" >작성일</td> 
	      <td align="center"  width="50" >조회수</td> 
	      <td align="center"  width="100" >IP</td>    
	    </tr>
	  </table>
</body>
</html>

</body>
</html>