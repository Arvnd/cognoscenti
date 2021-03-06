<%@page contentType="text/html;charset=UTF-8" pageEncoding="ISO-8859-1" session="true"
%><%@page errorPage="error.jsp"
%><%@page import="org.socialbiz.cog.AuthRequest"
%><%@page import="org.socialbiz.cog.NGPage"
%><%@page import="org.socialbiz.cog.NGBook"
%><%@page import="org.socialbiz.cog.NGPageIndex"
%><%@page import="org.socialbiz.cog.UtilityMethods"
%>
<%
    //constructing the AuthRequest object should always be the first thing
    //that a page does, so that everything can be set up correctly.
    AuthRequest ar = AuthRequest.getOrCreate(request, response, out);
    ar.assertLoggedIn("Unable to set to watch this page.");

    UserProfile uProf = ar.getUserProfile();

    String p = ar.reqParam("p");
    String action = ar.reqParam("action");
    String go = ar.reqParam("go");
    ngp = ar.getCogInstance().getProjectByKeyOrFail(p);
    ar.setPageAccessLevels(ngp);

    if ("Start Watching".equals(action))
    {
        uProf.setWatch(ngp.getKey(), ar.nowTime);
    }
    else if ("Reset Watch Time".equals(action))
    {
        uProf.setWatch(ngp.getKey(), ar.nowTime);
    }
    else if ("Stop Watching".equals(action))
    {
        uProf.clearWatch(ngp.getKey());
    }
    else
    {
        throw new Exception("Don't understand action: "+action);
    }

    uProf.setLastUpdated(ar.nowTime);
    UserManager.writeUserProfilesToFile();

    response.sendRedirect(go);
%>
<%@ include file="functions.jsp"%>