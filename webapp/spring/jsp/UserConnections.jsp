<%@page errorPage="/spring/jsp/error.jsp"
%><%@ include file="include.jsp"
%><%@page import="org.socialbiz.cog.dms.CVSConfig"
%><%@page import="org.socialbiz.cog.dms.ConnectionSettings"
%><%@page import="org.socialbiz.cog.dms.FolderAccessHelper"
%><%@page import="org.socialbiz.cog.dms.LocalFolderConfig"
%><%
    ar.assertLoggedIn("Must be logged in to see anything about a user");

    UserProfile uProf = (UserProfile)request.getAttribute("userProfile");
    if (uProf == null) {
        throw new NGException("nugen.exception.cant.find.user",null);
    }

    UserProfile  operatingUser =ar.getUserProfile();
    if (operatingUser==null) {
        //this should never happen, and if it does it is not the users fault
        throw new ProgramLogicError("user profile setting is null.  No one appears to be logged in.");
    }

    boolean viewingSelf = uProf.getKey().equals(operatingUser.getKey());

    List<CVSConfig> cvsConnectionList =  FolderAccessHelper.getCVSConnections();
    List<LocalFolderConfig> lclConnectionList =  FolderAccessHelper.getLoclConnections();

    JSONArray localConnections = new JSONArray();
    JSONArray cvsConnections = new JSONArray();
    for (LocalFolderConfig cc : lclConnectionList) {
        JSONObject jo = new JSONObject();
        jo.put("name", cc.getDisplayName());
        jo.put("path", cc.getPath());
        localConnections.put(jo);
    }
    for (CVSConfig cc : cvsConnectionList) {
        JSONObject jo = new JSONObject();
        jo.put("root", cc.getRoot());
        jo.put("sandbox", cc.getSandbox());
        jo.put("repository", cc.getRepository());
        cvsConnections.put(jo);
    }
%>

<script type="text/javascript">

var app = angular.module('myApp', ['ui.bootstrap']);
app.controller('myCtrl', function($scope, $http) {
    $scope.localConnections = <%localConnections.write(out,2,4);%>;
    $scope.cvsConnections = <%cvsConnections.write(out,2,4);%>;



    $scope.showError = false;
    $scope.errorMsg = "";
    $scope.errorTrace = "";
    $scope.showTrace = false;
    $scope.reportError = function(serverErr) {
        errorPanelHandler($scope, serverErr);
    };

    $scope.noImpl = function() {
        alert('no implemented yet');
    }

});
</script>

<!-- MAIN CONTENT SECTION START -->
<div ng-app="myApp" ng-controller="myCtrl">

<%@include file="ErrorPanel.jsp"%>

    <div class="generalHeading" style="height:40px">
        <div  style="float:left;margin-top:8px;">
            CVS Repository Connections
        </div>
        <div class="rightDivContent" style="margin-right:100px;">
          <span class="dropdown">
            <button class="btn btn-default dropdown-toggle" type="button" id="menu1" data-toggle="dropdown">
            Options: <span class="caret"></span></button>
            <ul class="dropdown-menu" role="menu" aria-labelledby="menu1">
              <li role="presentation"><a role="menuitem" tabindex="-1"
                  href="" ng-click="noImpl()">Add CVS Connection</a></li>
              <li role="presentation"><a role="menuitem" tabindex="-1"
                  href="" ng-click="noImpl()">Add Local Connection</a></li>
            </ul>
          </span>

        </div>
    </div>


    <table class="table">
        <tr>
            <th>Root</th>
            <th>Repository</th>
            <th>Sandbox</th>
            <th>Delete</th>
        </tr>

        <tr ng-repeat="row in cvsConnections">
            <td>{{row.root}}</td>
            <td>{{row.repository}}</td>
            <td>{{row.sandbox}}</td>
        </tr>

    </table>

    <div class="generalHeading" style="height:40px">
        Local Path Connections
    </div>


    <table class="table">
        <tr>
            <th>Name</th>
            <th>Path</th>
            <th>Delete</th>
        </tr>

        <tr ng-repeat="row in cvsConnections">
            <td>{{row.name}}</td>
            <td>{{row.path}}</td>
        </tr>

    </table>

