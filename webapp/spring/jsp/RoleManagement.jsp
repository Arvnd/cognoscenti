<%@page errorPage="/spring/jsp/error.jsp"
%><%@ include file="include.jsp"
%><%

    ar.assertLoggedIn("Must be logged in to edit roles");

    String pageId      = ar.reqParam("pageId");
    String siteId      = ar.reqParam("siteId");
    
    //page must work for both workspaces and for sites
    NGContainer ngc = ar.getCogInstance().getWorkspaceOrSiteOrFail(siteId, pageId);
    ar.setPageAccessLevels(ngc);
    UserProfile uProf = ar.getUserProfile();

    JSONArray allRoles = new JSONArray();

    for (NGRole aRole : ngc.getAllRoles()) {
        JSONObject rollo = new JSONObject();
        rollo.put("name", aRole.getName());
        rollo.put("color", aRole.getColor());
        List<AddressListEntry> players = aRole.getExpandedPlayers(ngc);
        rollo.put("count", players.size());
        JSONArray playlist = new JSONArray();
        for (AddressListEntry ale: players) {
            if (ale.getName().length()>0) {
                playlist.put(ale.getJSON());
            }
        }
        rollo.put("players", playlist);
        allRoles.put(rollo);
    }

    JSONArray allPeople = UserManager.getUniqueUsersJSON();


%>

<script type="text/javascript">

var app = angular.module('myApp', ['ui.bootstrap','ngTagsInput']);
app.controller('myCtrl', function($scope, $http, $modal) {
    $scope.allRoles = <%allRoles.write(out,2,4);%>;
    $scope.roleInfo = {};
    $scope.showInput = false;
    $scope.allPeople = <%allPeople.write(out,2,4);%>;
    $scope.colors = ["salmon","khaki","beige","lightgreen","orange","bisque","tomato","aqua","orchid","peachpuff","powderblue","lightskyblue"];

    $scope.inviteMsg = "Hello,\n\nYou have been asked by '<%ar.writeHtml(uProf.getName());%>' to"
                    +" participate in a role of the project '<%ar.writeHtml(ngc.getFullName());%>'."
                    +"\n\nThe links below will make registration quick and easy, and after that you will be able to"
                    +" participate directly with the others through the site.";
    
    $scope.showInput = false;
    $scope.showError = false;
    $scope.errorMsg = "";
    $scope.errorTrace = "";
    $scope.showTrace = false;
    $scope.reportError = function(serverErr) {
        errorPanelHandler($scope, serverErr);
    };

    $scope.fetchRole = function(selectedName) {
        $scope.selectedPersonShow = false;
        var postURL = "roleUpdate.json?op=Update";
        var rec = {};
        rec.name = selectedName;
        var postdata = angular.toJson(rec);
        $http.post(postURL ,postdata)
        .success( function(data) {
            $scope.isNew = false;
            $scope.showInput = true;
            $scope.roleInfo = data;
        })
        .error( function(data, status, headers, config) {
            $scope.reportError(data);
        });
    }


    $scope.createRole = function() {
        $scope.isNew = true;
        $scope.showInput = true;
        $scope.roleInfo = {};
    }


    $scope.updateRole = function() {
        var key = $scope.roleInfo.name;
        var postURL = "roleUpdate.json?op=Update";
        $scope.roleInfo.players.forEach( function(item) {
            if (!item.uid) {
                item.uid = item.name;
            }
        });
        console.log("SAVING: ", $scope.roleInfo);
        var postdata = angular.toJson($scope.roleInfo);
        if ($scope.isNew) {
            $scope.allRoles.push({name: key,color: $scope.roleInfo.color});
            postURL = "roleUpdate.json?op=Create";
        }
        else {
            $scope.allRoles.forEach( function(aRole) {
                if (aRole.name == key) {
                    aRole.color  = $scope.roleInfo.color;
                    aRole.players  = $scope.roleInfo.players;
                }
            });
        }
        $scope.showError=false;
        $http.post(postURL ,postdata)
        .success( function(data) {
            $scope.roleInfo = data;
            var newRoles = [];
            $scope.allRoles.forEach( function (item) {
                if (item.name==key) {
                    newRoles.push(data);
                }
                else {
                    newRoles.push(item);
                }
            });
            $scope.allRoles = newRoles;
        })
        .error( function(data, status, headers, config) {
            $scope.reportError(data);
        });
    };
    $scope.saveCreatedRole = function() {
        var key = $scope.roleInfo.name;
        var newOne = {name: key,color: $scope.roleInfo.color};
        var postdata = angular.toJson(newOne);
        $scope.allRoles.push(newOne);
        postURL = "roleUpdate.json?op=Create";
        $http.post(postURL ,postdata)
        .success( function(data) {
            $scope.roleInfo = data;
            var newRoles = [];
            $scope.allRoles.forEach( function (item) {
                if (item.name==key) {
                    newRoles.push(data);
                }
                else {
                    newRoles.push(item);
                }
            });
            $scope.allRoles = newRoles;
        })
        .error( function(data, status, headers, config) {
            $scope.reportError(data);
        });        
        $scope.isNew = false;
        $scope.showInput = false;
    }
    $scope.closePanel = function() {
        $scope.roleInfo = {};
        $scope.showInput = false;
    };
    $scope.getPeople = function(filter) {
        var lcfilter = filter.toLowerCase();
        var res = [];
        var last = $scope.allPeople.length;
        for (var i=0; i<last; i++) {
            var rec = $scope.allPeople[i];
            if (rec.name.toLowerCase().indexOf(lcfilter)>=0) {
                res.push(rec);
            }
        }
        return res;
    }
    $scope.addPlayer = function() {
        var found = false;
        var player = $scope.newPlayer;
        if (typeof player == "string") {
            var pos = player.lastIndexOf(" ");
            var name = player.substring(0,pos).trim();
            var uid = player.substring(pos).trim();
            player = {name: name, uid: uid};
        }
        $scope.roleInfo.players.forEach( function(one) {
            if (player.uid == one.uid) {
                found = true;
            }
        });
        $scope.newPlayer = "";
        if (found) {
            alert("That user is already a player of this role.");
            return;
        }
        $scope.roleInfo.players.push(player);
        $scope.updateRole();
        var isNew = true;
        $scope.allPeople.forEach( function(existingUser) {
            if (existingUser.uid == player.uid) {
                if (existingUser.key) {
                    isNew = false;
                }
            }
        });
        if (isNew) {
            //prompt to send an invite
            $scope.openInviteSender(player);
        }
    }
    $scope.removePlayer = function(player) {
        var res = $scope.roleInfo.players.filter( function(one) {
            return (player.uid != one.uid);
        });
        $scope.roleInfo.players = res;
    }
    $scope.visitPlayer = function(player) {
        window.location = "<%=ar.retPath%>v/FindPerson.htm?uid="+player.uid;
    }
    $scope.deleteRole = function() {
        var key = $scope.roleInfo.name;
        var ok = confirm("Are you sure you want to delete: "+key);
        var postURL = "roleUpdate.json?op=Delete";
        var postdata = angular.toJson($scope.roleInfo);
        $scope.showError=false;
        if (ok) {
            $http.post(postURL ,postdata)
            .success( function(data) {
                var newSet = [];
                $scope.allRoles.map( function(item) {
                    if (item.name!=key) {
                        newSet.push(item);
                    }
                });
                $scope.allRoles = newSet;
                $scope.closePanel();
            })
            .error( function(data, status, headers, config) {
                $scope.reportError(data);
            });
        }
    }
    $scope.bestPart = function(rec) {
        var name = rec.name;
        if (name) {
            return name;
        }
        return rec.uid;
    }
    $scope.imageName = function(player) {
        if (player.key) {
            return player.key+".jpg";
        }
        else {
            var lc = player.uid.toLowerCase();
            var ch = lc.charAt(0);
            var i =1;
            while(i<lc.length && (ch<'a'||ch>'z')) {
                ch = lc.charAt(i); i++;
            }
            return "fake-"+ch+".jpg";
        }
    }

    $scope.sendEmailLoginRequest = function(message) {
        var postURL = "<%=ar.getSystemProperty("identityProvider")%>?openid.mode=apiSendInvite";
        var postdata = JSON.stringify(message);
        $http.post(postURL ,postdata)
        .success( function(data) {
            alert("message has been sent to "+message.userId);
        })
        .error( function(data, status, headers, config) {
            $scope.reportError(data);
        });
    }

    $scope.loadItems = function(query) {
        var res = [];
        var q = query.toLowerCase();
        $scope.allPeople.forEach( function(person) {
            if (person.name.toLowerCase().indexOf(q)<0 && person.uid.toLowerCase().indexOf(q)<0) {
                return;
            }
            var nix = {};
            nix.name = person.name; 
            nix.uid  = person.uid; 
            nix.key  = person.key; 
            res.push(nix);
        });
        return res;
    }
    $scope.toggleSelectedPerson = function(tag) {
        $scope.selectedPersonShow = !$scope.selectedPersonShow;
        $scope.selectedPerson = tag;
        if (!$scope.selectedPerson.uid) {
            $scope.selectedPerson.uid = $scope.selectedPerson.name;
        }
    }
    $scope.navigateToUser = function(player) {
        window.location="<%=ar.retPath%>v/FindPerson.htm?uid="+encodeURIComponent(player.uid);
    }
    
    $scope.openInviteSender = function (player) {

        var modalInstance = $modal.open({
            animation: false,
            templateUrl: '<%=ar.retPath%>templates/InviteModal.html?t=<%=System.currentTimeMillis()%>',
            controller: 'InviteModalCtrl',
            size: 'lg',
            backdrop: "static",
            resolve: {
                email: function () {
                    return player.uid;
                },
                msg: function() {
                    return $scope.inviteMsg;
                }
            }
        });

        modalInstance.result.then(function (message) {
            $scope.inviteMsg = message.msg;
            message.userId = player.uid;
            message.name = player.name;
            message.return = "<%=ar.baseURL%><%=ar.getResourceURL(ngc, "frontPage.htm")%>";
            $scope.sendEmailLoginRequest(message);
        }, function () {
            //cancel action - nothing really to do
        });
    };

});

</script>

<div ng-app="myApp" ng-controller="myCtrl">

<%@include file="ErrorPanel.jsp"%>

    <div class="generalHeading" style="height:40px">
        <div  style="float:left;margin-top:8px;">
            Roles of Workspace
        </div>
        <div class="rightDivContent" style="margin-right:100px;">
          <span class="dropdown">
            <button class="btn btn-default dropdown-toggle" type="button" id="menu1" data-toggle="dropdown">
            Options: <span class="caret"></span></button>
            <ul class="dropdown-menu" role="menu" aria-labelledby="menu1">
              <li role="presentation"><a role="menuitem" tabindex="-1"
                  ng-click="createRole()">Create New Role</a></li>
            </ul>
          </span>

        </div>
    </div>

    <style>
    .spacey tr td{
        padding: 8px;
    }
    .spacey {
        width: 100%;
    }
    </style>
    
    <table class="spacey"><tr>
        <td style="width:400px;height:600px;vertical-align:top;" >
            <table>
                <tr ng-repeat="role in allRoles" style="background-color:{{(role.name==roleInfo.name)?'#EEE':'white'}};" class="generalContent">
                    <td style="padding:10px">
                        <button class="btn btn-sm" style="color:black;background-color:{{role.color}}"
                             ng-click="fetchRole(role.name)">{{role.name}}</button>
                    </td>
                    <td style="align:right;padding:10px">
                        <span ng-repeat="player in role.players">
                            <img sh-show="player.key" class="img-circle" src="<%=ar.retPath%>users/{{imageName(player)}}" style="width:32px;height:32px"
                            title="{{player.name}} - {{player.uid}}">
                        </span>
                    </td>
                </tr>
            </table>

        </td>
        <td  ng-hide="showInput" style="vertical-align:top;text-align:center;" >
            <i>select a role to edit, be sure to save to preserve changes</i>
        </td>
        <td  class="well" ng-show="showInput && !isNew" style="vertical-align:top;" >
            <table width="100%">
                <tr><td style="height:10px" colspan="3"></td></tr>
                <tr>
                    <td class="gridTableColummHeader">Name:</td>
                    <td>
                       <input ng-show="isNew" class="form-control" ng-model="roleInfo.name">
                       <button ng-hide="isNew" class="btn btn-sm" style="background-color:{{roleInfo.color}};">{{roleInfo.name}}</button>
                    </td>
                    <td align="right">
                       <div class="dropdown" style="float: right;">
                            <b>Color:</b>
                            <button class="btn btn-default dropdown-toggle" type="button" id="menu2"
                                data-toggle="dropdown" style="background-color:{{roleInfo.color}};">
                            {{roleInfo.color}} <span class="caret"></span></button>
                            <ul class="dropdown-menu" role="menu" aria-labelledby="menu2">
                                <li role="presentation" ng-repeat="color in colors">
                                    <a role="menuitem" style="background-color:{{color}};"
                                    ng-click="roleInfo.color=color">{{color}}</a></li>
                            </ul>
                        </div>
                    </td>
                    <td style="width:30px;"></td>
                </tr>
                <tr>
                    <td class="gridTableColummHeader">Players:</td>
                    <td colspan="2">
                      <tags-input ng-model="roleInfo.players" placeholder="Enter user name or id"
                                  display-property="name" key-property="uid" 
                                  on-tag-clicked="toggleSelectedPerson($tag)">
                          <auto-complete source="loadItems($query)"></auto-complete>
                      </tags-input>
                    </td>
                </tr>
                <tr ng-show="selectedPersonShow">
                    <td></td>
                    <td class="well" colspan="2"> 
                       for <b>{{selectedPerson.name}}</b>:
                       <button ng-click="navigateToUser(selectedPerson)" class="btn btn-info">
                           Visit Profile</button>
                       <button ng-click="openInviteSender(selectedPerson)" class="btn btn-info">
                           Invite</button>
                       <button ng-click="selectedPersonShow=false" class="btn">
                           Hide</button>
                    </td>
                </tr>
                <tr>
                     <td class="gridTableColummHeader">Description:</td>
                     <td colspan="2"><textarea ng-model="roleInfo.description" placeholder="Enter Description of Role"
                         class="form-control" style="height:150px;"></textarea></td>
                    <td style="width:30px;"></td>
                </tr>
                <tr>
                     <td class="gridTableColummHeader">Eligibility:</td>
                     <td colspan="2"><textarea ng-model="roleInfo.requirements" placeholder="Enter Eligibility Requirements"
                         style="height:150px;" class="form-control"></textarea></td>
                    <td style="width:30px;"></td>
                </tr>
                <tr>
                     <td class="gridTableColummHeader"></td>
                     <td colspan="2"><button ng-click="updateRole()" class="btn btn-primary">Save Changes</button>
                     <button ng-click="closePanel()" class="btn btn-primary">Cancel</button>
                         &nbsp; &nbsp; &nbsp;
                     <button ng-click="deleteRole()" class="btn btn-primary">Delete Role</button>
                     </td>
                    <td style="width:30px;"></td>
                </tr>
            </table>
        </td>
        <td  class="well" ng-show="showInput && isNew" style="vertical-align:top;" >
            <table width="100%">
                <tr><td style="height:10px" colspan="3"></td></tr>
                <tr>
                    <td class="gridTableColummHeader">Name:</td>
                    <td>
                       <input ng-show="isNew" class="form-control" ng-model="roleInfo.name">
                    </td>
                </tr>
                <tr>
                     <td class="gridTableColummHeader"></td>
                     <td colspan="2"><button ng-click="saveCreatedRole()" class="btn btn-primary">Create Role</button>
                     </td>
                </tr>
            </table>
        </td>    </tr></table>


</div>
<script src="<%=ar.retPath%>templates/InviteModal.js"></script>

