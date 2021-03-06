<%@page errorPage="/spring/jsp/error.jsp"
%><%@ include file="/spring/jsp/include.jsp"
%><%

    ar.assertLoggedIn("Must be logged in to see a list of meetings");
    
    String searchText = ar.defParam("s", "");

%>

<script type="text/javascript">

var app = angular.module('myApp', ['ui.bootstrap']);
app.controller('myCtrl', function($scope, $http) {
    $scope.results = [];
    $scope.query = {
        searchFilter: "<% ar.writeJS(searchText); %>",
        searchSite: "all",
        searchProject: "all"
    }

    $scope.showError = false;
    $scope.errorMsg = "";
    $scope.errorTrace = "";
    $scope.showTrace = false;
    $scope.reportError = function(serverErr) {
        errorPanelHandler($scope, serverErr);
    };
    $scope.hasResults = false;
    $scope.isSearching = true;
    $scope.actualSearch = "";

    $scope.doSearch = function() {
        var postURL = "searchNotes.json";
        var postdata = angular.toJson($scope.query);
        $scope.showError=false;
        $scope.isSearching = true;
        $http.post(postURL ,postdata)
        .success( function(data) {
            $scope.results = data;
            $scope.hasResults = ($scope.results.length>0);
            $scope.isSearching = false;
            $scope.actualSearch = $scope.query.searchFilter;
        })
        .error( function(data, status, headers, config) {
            $scope.reportError(data);
        });
    };
    
    $scope.clearResults = function() {
        $scope.results = [];
        $scope.hasResults = false;
        $scope.actualSearch = "";
    }

    <% if (searchText.length()>0) { %>
    $scope.doSearch();
    <% } %>
});
</script>

<!-- MAIN CONTENT SECTION START -->
<div ng-app="myApp" ng-controller="myCtrl">

<%@include file="ErrorPanel.jsp"%>

    <div class="generalHeading" style="height:40px">
        <div  style="float:left;margin-top:8px;">
            Search All Topics
        </div>
        <div class="rightDivContent" style="margin-right:100px;">
          <span class="dropdown">
            <button class="btn btn-default dropdown-toggle" type="button" id="menu1" data-toggle="dropdown">
            Options: <span class="caret"></span></button>
            <ul class="dropdown-menu" role="menu" aria-labelledby="menu1">
              <li role="presentation"><a role="menuitem" tabindex="-1"
                  ng-click="clearResults()">Clear Results</a></li>
            </ul>
          </span>

        </div>
    </div>

    <table>
        <tr >
            <td class="gridTableColummHeader">Search For:</td>
            <td style="width:20px;"></td>
            <td><input ng-model="query.searchFilter" class="form-control" style="width:450px;"></td>
        </tr>
        <tr ><td height="10px"></td></tr>
        <tr >
            <td class="gridTableColummHeader">Workspaces:</td>
            <td style="width:20px;"></td>
            <td>
              <div class="form-inline form-group" style="margin:0px">
                  <select ng-model="query.searchProject" class="form-control" style="width:150px;">>
                      <option value="all">All Workspaces</option>
                      <option value="member">Member Workspaces</option>
                      <option value="owner">Owned Workspaces</option>
                  </select>
                  <span>
                      <span class="gridTableColummHeader">Sites:</span>
                      <select ng-model="query.searchSite" class="form-control" style="width:150px;">
                          <option value="one">This Site</option>
                          <option value="all">All Sites</option>
                      </select>
                  </span>
              </div>
            </td>
        </tr>
        <tr ><td height="10px"></td></tr>
        <tr >
            <td class="gridTableColummHeader"></td>
            <td style="width:20px;"></td>
            <td><button ng-click="doSearch()" class="btn btn-primary">Search</button></td>
        </tr>
    </table>

    <div style="height:60px"></div>

    <table class="table" width="100%">
        <tr class="gridTableHeader">
            <td width="200px">Site/Workspace</td>
            <td width="200px">Topic</td>
            <td width="100px">Updated</td>
        </tr>
        <tr ng-repeat="row in results">
            <td>{{row.siteName}} / <a href="<%=ar.retPath%>{{row.projectLink}}">{{row.projectName}}</a></td>
            <td><a href="<%=ar.retPath%>{{row.noteLink}}">{{row.noteSubject}}</a></td>
            <td>{{row.modTime | date}}</td>
        </tr>
        <tr ng-hide="hasResults">
           <td colspan="5">
           <div class="guideVocal" ng-hide="isSearching"> 
             Did not find any results for search string: {{actualSearch}}
           </div>
           <div class="guideVocal" ng-show="isSearching"> 
             Searching for results for string: {{actualSearch}}
           </div>
           </td>
        </tr>
    </table>


</div>
<!-- MAIN CONTENT SECTION END -->
