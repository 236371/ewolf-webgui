EWOLF_CONSTANTS = {
	REFRESH_INTERVAL_SEC : 60,
	LOADING_FRAME : "loadingFrame",
	APPLICATION_FRAME : "applicationFrame",
	MAIN_FRAME : "mainFrame",
	MENU_FRAME : "menu",
	TOPBAR_FRAME : "topbarID",
	
	WELCOME_MENU_ID : "__welcome_menu__",
	MAINAPPS_MENU_ID : "__mainapps_menu__",
	WOLFPACKS_MENU_ID : "__wolfpacks_menu__",
	
	MYPROFILE_APP_ID : "profile",
	NEWSFEED_APP_ID : "newsfeed",
	INBOX_APP_ID : "inbox",
	LOGIN_APP_ID : "login",
	SIGNUP_APP_ID : "signup",
	LOGOUT_APP_ID : "logout",
	
	FIRST_EWOLF_LOGIN_REQUEST_ID : "eWolfLogin",
	
	PROFILE_REQUEST_NAME : "__main_profile_request__",
	WOLFPACKS_REQUEST_NAME : "__main_wolfpacks_request",
	MEMBERS_REQUEST_NAME : "__main_members_request__",
	APPROVED_MEMBERS_REQUEST_NAME : "__pending_requests_approved_request__",
	
	APPROVED_WOLFPACK_NAME : "wall-readers",
	APPROVED_ME_WOLFPACK_NAME : "followers",
	
	INBOX_MAX_OLDER_MESSAGES_FETCH : 2,
	NEWSFEED_MAX_OLDER_MESSAGES_FETCH : 2,
	
	REQUEST_CATEGORY_INBOX : "inbox",
	REQUEST_CATEGORY_WOLFPACKS : "wolfpacks",
	REQUEST_CATEGORY_WOLFPACKS_ALIAS1 : "wolfpacksAll",
	REQUEST_CATEGORY_PROFILE : "profile",
	REQUEST_CATEGORY_WOLFPACK_MEMBERS : "wolfpackMembers",
	REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS1 : "wolfpackMembersAll",
	REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS2 : "wolfpackMembersNotAllowed",
	REQUEST_CATEGORY_NEWS_FEED : "newsFeed",
	REQUEST_CATEGORY_CREATE_WOLFPACK : "createWolfpack",
	REQUEST_CATEGORY_ADD_WOLFPACK_MEMBER : "addWolfpackMember",
	REQUEST_CATEGORY_POST : "post",
	REQUEST_CATEGORY_SEND_MESSAGE : "sendMessage",
	REQUEST_CATEGORY_CREATE_ACCOUNT : "createAccount",
	REQUEST_CATEGORY_LOGIN : "login",
	REQUEST_CATEGORY_LOGOUT : "logout",
};

WOLFPACK_CONSTANTS = {
	WOLFPACK_APP_PREFIX : "wolfpack:"
};

CREATE_NEW_WOLFPACK_LINK_CONSTANTS = {
	QUERY_ID : "query"	
};

SEARCHBAR_CONSTANTS = {
	SEARCH_PROFILE_PREFIX : "profile:",
	SEARCH_MENU_ITEM_ID : "__seach_menu_id__"
};

SIGNUP_CONSTANTS = {
	SIGNUP_FULL_NAME_ID : "signup full name",
	SIGNUP_USERNAME_ID : "signup username",
	SIGNUP_PASSWORD_ID : "signup password",
	SIGNUP_VERIFY_PASSWORD_ID : "signup verify password"
};

LOGIN_CONSTANTS = {
	LOGIN_USERNAME_ID : "login username",
	LOGIN_PASSWORD_ID : "login password"
};

NEWMAIL_CONSTANTS = {
	NEWMAIL_APP_ID_PREFIX : "mailto:",
	NEW_MAIL_DAFAULTS : {
			TITLE : "New Mail",
			TO : "To",
			CONTENT : "Content",
			ATTACHMENT : "Attachment"
		}
};
var eWolf = new function() {
	var self = this;
	$.extend(this,EWOLF_CONSTANTS);
	
	this.userID = null;
	this.userName = null;
	this.selectedApp = null;
	
	this.serverRequest = null;
	
	this.mainAppsCreated = false;
	
	this.init = function() {
		self.serverRequest = new PostRequestHandler("/json",self.REFRESH_INTERVAL_SEC);
		
		$(window).bind('hashchange', self.onHashChange);
		
		self.applicationFrame = $("#"+self.APPLICATION_FRAME);
		self.mainFrame = $("#"+self.MAIN_FRAME);
		self.topBarFrame = $("#"+self.TOPBAR_FRAME);
		self.menuFrame = $("#"+self.MENU_FRAME);
		self.loadingFrame = $("#"+self.LOADING_FRAME);
		
		new Loading(self.loadingFrame);		
		
		self.sideMenu = new SideMenu(self.menuFrame,
				self.mainFrame);
		
		self.welcome = self.sideMenu.createNewMenuList(
				self.WELCOME_MENU_ID,"Welcome");
		
		self.mainApps = self.sideMenu.createNewMenuList(
				self.MAINAPPS_MENU_ID,"Main");
		
		self.wolfpacksMenuList = self.sideMenu.createNewMenuList(
				self.WOLFPACKS_MENU_ID,"Wolfpacks");
		
		self.serverRequest.registerRequest(self.PROFILE_REQUEST_NAME,
				function() {
					return { profile : {}	};
				});
		
		self.serverRequest.registerRequest(self.WOLFPACKS_REQUEST_NAME,
				function() {
					return { wolfpacksAll : {}	};
				});
		
		self.serverRequest.bindRequest(self.PROFILE_REQUEST_NAME, self.FIRST_EWOLF_LOGIN_REQUEST_ID);
		self.serverRequest.bindRequest(self.WOLFPACKS_REQUEST_NAME);
		
		self.members = new Members();		
		self.wolfpacks = new Wolfpacks(self.wolfpacksMenuList,self.applicationFrame);
		self.profile = new Profile(self.MYPROFILE_APP_ID,self.applicationFrame);
		
		self.serverRequest.registerHandler(self.PROFILE_REQUEST_NAME,
				new ResponseHandler("profile",["id","name"],
						function (data, textStatus, postData) {
					document.title = "eWolf - " + data.name;
					self.userID = data.id;
					self.userName = data.name;
				}).getHandler());
		
		self.serverRequest.addOnComplete(null,function(appID, response, status) {
			if(self.mainAppsCreated) {
				if(response.status != 200 || self.userID == null) {
					document.location.reload(true);
				}				
			} else if(response.status == 200 && self.userID != null) {
				self.serverRequest.restartRefreshInterval();
				self.createMainApps();
			} else if(response.status != 200 || self.userID == null) {
				self.serverRequest.stopRefreshInterval();
				self.presentLoginScreen();
			}
		});
		
		self.getUserInformation();
	};
	
	this.getUserInformation = function () {
		if(self.loginApp) {
			self.loginApp.destroy();
			self.loginApp = null;
		}
		
		if(self.signupApp) {
			self.signupApp.destroy();
			self.signupApp = null;
		}
		
		self.serverRequest.requestAll(self.FIRST_EWOLF_LOGIN_REQUEST_ID, true);
	};
	
	this.createMainApps = function () {
		self.mainAppsCreated = true;
		
		self.welcome.hideMenu();
		self.logout = new Logout(self.LOGOUT_APP_ID,"Logout",eWolf.topBarFrame);
		
		self.mainApps.addMenuItem(self.MYPROFILE_APP_ID,"My Profile");
				
		self.mainApps.addMenuItem(self.NEWSFEED_APP_ID,"News Feed");
		self.newsFeedApp = new WolfpackPage(self.NEWSFEED_APP_ID,null,self.applicationFrame);
		
		self.mainApps.addMenuItem(self.INBOX_APP_ID,"Messages");
		self.inboxApp = new Inbox(self.INBOX_APP_ID,self.applicationFrame);
		
		self.pendingRequests = new PendingRequests(self.topBarFrame);
		
		self.searchBar = new SearchBar(self.sideMenu,
				self.applicationFrame,self.topBarFrame);
		
		self.serverRequest.setRequestAllOnSelect(true);
		self.onHashChange();
	};
	
	this.presentLoginScreen = function() {
		self.serverRequest.stopRefreshInterval();
		self.serverRequest.setRequestAllOnSelect(false);
		
		// Welcome
		self.welcome.addMenuItem(self.LOGIN_APP_ID,"Login");
		if(!self.loginApp) {
			self.loginApp = new Login(self.LOGIN_APP_ID,self.applicationFrame).select();
		}
		
		self.welcome.addMenuItem(self.SIGNUP_APP_ID,"Signup");
		if(!self.signupApp) {
			self.signupApp = new Signup(self.SIGNUP_APP_ID,self.applicationFrame);
		}
	};
	
	this.onHashChange = function() {
		if(window.location.hash && window.location.hash != "") {
			var selected = window.location.hash.replace("#", "");
			
			var found = false;
			
			$.each($(self).data("events").select, function(i,handler) {				
				if(handler.type == "select" && handler.namespace == selected) {
					found = true;
					return false;
				}
			});
			
			if(found) {
				self.trigger("select",[selected]);
			} else {
				var selectedSubString = selected.substring(0,
						self.searchBar.SEARCH_PROFILE_PREFIX.length);
				
				if(selectedSubString ==
					self.searchBar.SEARCH_PROFILE_PREFIX) {
					var searchTerm = selected.substring(selectedSubString.length);
					if(searchTerm != "") {
						self.trigger("search",[searchTerm]);
					} else {
						self.selectApp(self.NEWSFEED_APP_ID);
					}					
				} else {
					self.selectApp(self.NEWSFEED_APP_ID);
				}				
			}			
		} else {
			self.selectApp(self.NEWSFEED_APP_ID);
		}
	};
	
	this.selectApp = function (id) {
		var newHash = "#"+id;
		if(window.location.hash != newHash) {
			window.location.hash = newHash;
		} else {
			self.onHashChange();
		}
	};
	
	this.bind = function (arg0,arg1) {
		$(self).bind(arg0,arg1);
		return self;
	};
	
	this.unbind = function (arg0,arg1) {
		$(self).unbind(arg0,arg1);
		return self;
	};
	
	this.trigger = function (arg0,arg1) {
		$(self).trigger(arg0,arg1);
		return self;
	};
	
	this.bind("select",function(event,eventId) {
		self.selectedApp = eventId;
	});
	
	return this;
};

$(document).ready(function () {
	eWolf.init();	
});
var Members = function() {
	var self = this;
	
	this.knownUsersFullDescriptionArray = [];
	this.knownUsersIDArray = [];
	var knownUsersMapByID = {};
	
	var membersResponseHandler = new ResponseHandler("wolfpackMembers",
			["membersList"],handleMembers);
	
	eWolf.serverRequest.registerRequest(eWolf.MEMBERS_REQUEST_NAME,
			function() {
				return { wolfpackMembers : {}	};
			});
	
	eWolf.serverRequest.registerHandler(eWolf.MEMBERS_REQUEST_NAME,
			membersResponseHandler.getHandler());
	
	eWolf.serverRequest.bindRequest(eWolf.MEMBERS_REQUEST_NAME,
			eWolf.FIRST_EWOLF_LOGIN_REQUEST_ID);
	
	function handleMembers(data, textStatus, postData) {
		$.each(data.membersList, function(i,userObj){
			self.addKnownUsers(userObj.id,userObj.name);
		});
	}
	
	this.addKnownUsers = function(userID,userName) {
		if(!knownUsersMapByID[userID]) {
			knownUsersMapByID[userID] = userName;
			var fullDesc = userName+" ("+userID+")";
			self.knownUsersFullDescriptionArray.push(fullDesc);
			self.knownUsersIDArray.push(userID);
			
			eWolf.trigger("foundNewUser",[userID,userName,fullDesc]);
		}		
		
		return self;
	};
	
	this.getUserFromFullDescription = function (fullDescription) {
		var idx = self.knownUsersFullDescriptionArray.indexOf(fullDescription);
		if(idx != -1){
			return self.knownUsersIDArray[idx];
		} else {
			return null;
		}
	};
	
	this.getUserName = function (userID, onReady) {
		var itsName = knownUsersMapByID[userID];
		if(!itsName && onReady) {
			eWolf.serverRequest.request(null,{
						profile: {
							userID: userID
						}
					  },
					new ResponseHandler("profile",["name"],
							function(data, textStatus, postData) {
						self.addKnownUsers(userID,data.name);
						onReady(data.name);
					}).getHandler());
		}
		
		return itsName;
	};
	
	return this;
};var Wolfpacks = function (menuList,applicationFrame) {
	var self = this;
	$.extend(this,WOLFPACK_CONSTANTS);
	
	var wolfpacksApps = {},
		UID = 100;
	
	this.wolfpacksArray = [];
	
	menuList.addExtraItem(CreateNewWolfpackLink());
	
	var wolfpacksResponseHandler = new ResponseHandler("wolfpacksAll",
			["wolfpacksList"],handleWolfpacks);
	
	eWolf.serverRequest.registerHandler(eWolf.WOLFPACKS_REQUEST_NAME,
			wolfpacksResponseHandler.getHandler());
	
	function handleWolfpacks(data, textStatus, postData) {
		$.each(data.wolfpacksList, function(i,pack){
			self.addWolfpack(pack);
		});
	}
	
	this.addWolfpack = function (pack) {
		if(wolfpacksApps[pack] == null) {
			var packID = self.WOLFPACK_APP_PREFIX + pack + "_" + UID;
			UID += 1;
			packID = packID.replace(/[^a-zA-Z0-9_:]/g,'_');
			menuList.addMenuItem(packID,pack);			
			var app = new WolfpackPage(packID,pack,applicationFrame);			
			
			wolfpacksApps[pack] = app;
			self.wolfpacksArray.push(pack);
		}
		
		return self;
	};
	
	this.getWolfpackAppID = function(pack) {
		var app = wolfpacksApps[pack];
		if(app) {
			return app.getId();
		} else {
			return null;
		}
	};
	
	this.removeWolfpack = function(pack) {
		if(wolfpacksApps[pack] != null) {
			menuList.removeMenuItem("__pack__"+pack);
			wolfpacksApps[pack].destroy();
			wolfpacksApps[pack] = null;
			
			var idx = wolfpacksArray.indexOf(pack);
			if(idx != -1){
				wolfpacksArray.splice(idx, 1);
			}
		}
		
		return self;
	};
	
	this.createWolfpacks = function(wolfpacks,onComplete) {
		if(wolfpacks.length > 0) {			
			var responseHandler = new ResponseHandler("createWolfpack",[],null);
			
			responseHandler.success(function(data, textStatus, postData) {
				$.each(wolfpacks,function(i,pack) {
					self.addWolfpack(pack);
				});
			}).error(function(data, textStatus, postData) {				
				if(data.wolfpacksResult == null) {
					console.log("No wolfpacksResult in response");
				} else {
					$.each(data.wolfpacksResult, function(i,response) {
						if(response.result == RESPONSE_RESULT.SUCCESS) {
							self.addWolfpack(postData.wolfpackNames[i]);
						}
					});
				}
			}).complete(onComplete);
			
			eWolf.serverRequest.request("wolfpacks",{
				createWolfpack: {
					wolfpackNames: wolfpacks
				}
			},responseHandler.getHandler());
			
		} else {
			if(onComplete) {
				onComplete();
			}			
		}
	};
	
	return this;
};



var RESPONSE_RESULT = {
		SUCCESS :				"SUCCESS",
			//	if everything went well.
		BAD_REQUEST :			"BAD_REQUEST",
			//	Missing an obligatory parameter.
			//	Wrong type or format parameter.
		INTERNAL_SERVER_ERROR :	"INTERNAL_SERVER_ERROR", 
			//for any internal server error.
		ITEM_NOT_FOUND :		"ITEM_NOT_FOUND",
			//	if the requested item did not found (for any reason).
		GENERAL_ERROR :			"GENERAL_ERROR",
			// for errors from unknown reason (no one of above)
		UNAVAILBLE_REQUEST :	"UNAVAILBLE_REQUEST",
			// if the request category is unavailable or not exists.
};

var GenericResponse = function (obj) {
	var self = this;	
	$.extend(this,obj);
	
	var UNDEFINED_RESULT = "undifined result";
	
	this.isSuccess = function() {
		if(self.result) {
			return self.result == RESPONSE_RESULT.SUCCESS;
		} else {
			return false;
		}		
	};
	
	this.isGeneralError = function() {
		if(self.result) {
			return self.result == RESPONSE_RESULT.GENERAL_ERROR;
		} else {
			return false;
		}		
	};
	
	this.toString = function() {
		if((!self.result) && (!self.errorMessage)) {
			return UNDEFINED_RESULT;
		} else if(!self.result) {
			return self.errorMessage;
		} else if(!self.errorMessage) {
			return self.result;
		} else {
			return self.result + " : " + self.errorMessage;
		}
	};
	
	
	return this;
};var BasicRequestHandler = function(requestAddress,refreshIntervalSec) {
	var self = this;
	
	var requestsMap = {},
			appsRequests = {},
			generalRequests = [];

	var onCompleteAll = [];
	var onGeneralError = null;
	var timer = null;
	var requestAllOnSelect = false;
	
	this.stopRefreshInterval = function () {
		clearTimeout(timer);
	};
	
	this.restartRefreshInterval = function () {
		if(refreshIntervalSec > 0) {
			clearTimeout(timer);
			timer = setTimeout(timerTimeout,refreshIntervalSec*1000);
		}
	};
	
	function onRequestBegin (appID) {
		eWolf.trigger("loading",[appID]);
	}
	
	function onRequestComplete (appID, response, status) {
		eWolf.trigger("loadingEnd",[appID]);		
	}
	
	function omRequestAllBegin(appID) {
		self.stopRefreshInterval();
	}
	
	function onRequestAllComplete(appID, response, status) {
		self.restartRefreshInterval();
		
		if(appID && appsRequests[appID] && appsRequests[appID].onComplete) {
			appsRequests[appID].onComplete(appID, response, status);
		}
		
		if(onCompleteAll) {
			$.each(onCompleteAll, function(i, onCompleteFunc) {
				onCompleteFunc(appID, response, status);
			});			
		}		
	}
		
	function timerTimeout() {
		self.requestAll(eWolf.selectedApp,false);
	}
	
	eWolf.bind("select",function(event,eventId) {
		if(requestAllOnSelect) {
			self.requestAll(eventId,false);
		}		
	});
	
	eWolf.bind("refresh",function(event,eventId) {
		self.requestAll(eventId,true);
	});
	
	eWolf.bind("needRefresh",function(event,eventId) {
		if(eventId && appsRequests[eventId]) {
			$.each(appsRequests[eventId], function(i, req) {
				req.lastUpdate = 0;
			});
		}
	});
	
	this.setRequestAllOnSelect = function (enable) {
		requestAllOnSelect = enable;
	};
	
	this.registerRequest = function(requestName, requestFunction) {
		if(requestName && requestFunction) {
			requestsMap[requestName] = {
					request : requestFunction,
					handlers : [],
					lastUpdate : 0
			};
		}
		
		return self;
	};
	
	this.registerHandler = function(requestName, handleDataFunction) {
		if(requestName && requestsMap[requestName]) {
			requestsMap[requestName].handlers.push(handleDataFunction);
		}
		
		return self;
	};
	
	this.bindRequest = function(requestName,appID) {
		if(requestName && requestsMap[requestName]) {
			if(appID) {
				if(!appsRequests[appID]) {
					appsRequests[appID] = [];
				}
				appsRequests[appID].push(requestsMap[requestName]);
			} else {
				generalRequests.push(requestsMap[requestName]);
			}			
		}
		
		return self;
	};
	
	this.bindAppToAnotherApp = function(newAppID, existsAppID) {
		if(existsAppID && appsRequests[existsAppID]) {
			if(newAppID) {
				appsRequests[newAppID] = appsRequests[existsAppID];
			}
		}
		
		return self;
	};
	
	this.unregisterApp = function(appID) {
		if(appID && appsRequests[appID]) {
			delete appsRequests[appID];
		}
		
		return self;
	};
	
	this.setRequestAddress = function(inputRequestAddress) {
		requestAddress = inputRequestAddress;
		return self;
	};
	
	this._makeRequest = function (address,data,success) {
		return self;
	};
	
	this.request = function(appID,data,handleDataFunction,handleOnComplete) {
		onRequestBegin(appID);
		
		self._makeRequest(requestAddress,data,
			function(receivedData,textStatus) {
				if(handleDataFunction != null) {
					handleDataFunction(receivedData,textStatus,data);
				}				
			}).complete(function(response, status) {
				onRequestComplete(appID, response, status);

				if(handleOnComplete != null) {
					handleOnComplete(appID, response, status);
				}				
			}).error(function(response, status, xhr){ 
				if(onGeneralError) {
					onGeneralError(response, status, xhr);
				}
			});
		
		return self;
	};	

	this.requestObjectArray = function(appID,requestsObj,handleOnComplete) {
		if(requestsObj.length == 0) {
			return self;
		}
		
		var data = {};

		$(requestsObj).each(function(i, reqObj) {
			$.extend(data, reqObj.request());
		});

		this.request(appID, data, function(receivedData, textStatus, data) {
			$(requestsObj).each(function(i, reqObj) {
				$(reqObj.handlers).each(function(i, handlerFunc) {
					handlerFunc(receivedData, textStatus, data);
				});
				
				reqObj.lastUpdate = new Date().getTime();
			});
		}, handleOnComplete);

		return self;
	};
	
	this.requestAll = function(appID,forceUpdate) {
		omRequestAllBegin(appID);
		
		var requestsObj = generalRequests;
		
		if(appID && appsRequests[appID]) {
			requestsObj = requestsObj.concat(appsRequests[appID]);
		}

		var needRefresh = [];
		
		if(forceUpdate) {
			needRefresh = requestsObj;
		} else {
			needRefresh = self.filterByLastUpdate(requestsObj);
		}	
		
		return self.requestObjectArray(appID,needRefresh,
				onRequestAllComplete);
	};
	
	this.filterByLastUpdate = function(requestsObj) {
		var needRefresh = [];
		
		var needRefreshTime = new Date().getTime() - 
							(refreshIntervalSec * 1000);

		$(requestsObj).each(function(i,reqObj) {
			if(reqObj.lastUpdate < needRefreshTime) {
				needRefresh.push(reqObj);
			}
		});
		
		return needRefresh;
	};
	
	this.addOnComplete = function(appID,newOnComplete) {
		if(appID && appsRequests[appID]) {
			appsRequests[appID].onComplete = newOnComplete;
		} else {
			onCompleteAll.push(newOnComplete);
		}		
		
		return self;
	};
	
	this.error = function(newOnGeneralError) {
		if(newOnGeneralError) {
			onGeneralError = newOnGeneralError;
		}		
		
		return self;
	};
		
	return this;
};

var PostRequestHandler = function(requestAddress,refreshIntervalSec) {
	BasicRequestHandler.call(this,requestAddress,refreshIntervalSec);
	
	this._makeRequest = function (address,data,success) {
		return $.post(address,JSON.stringify(data),success,"json");
	};
	
	return this;
};

/*var JSONRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	BasicRequestHandler.call(this,id,requestAddress,refreshIntervalSec);
	
	this._makeRequest = function (address,data,success) {		
		return $.getJSON(address,data,success);
	};
	
	return this;
};*/

RESPONSE_ARRAY_CONDITION_GENRAL_ERROR = 
	function(response, textStatus, postData) {
	return response.isGeneralError();
};

var ResponseHandler = function(category, requiredFields, handler) {
	var thisObj = this;
	
	var errorHandler = null;
	var completeHandler = null;
	var badResponseHandler = null;
	var responseArray = [];
	
	function theHandler(data, textStatus, postData) {
		if (data[category]) {
			var response = new GenericResponse(data[category]);
			var valid = true;
			
			$.each(responseArray, function(i, resObj) {
				if(resObj.condition && resObj.key &&
						resObj.condition(response, textStatus, postData[category])) {
					if(response[resObj.key]) {
						$.each(response[resObj.key], function(pos, item) {
							var subResponse = new GenericResponse(item);
							
							if(subResponse.isSuccess()) {
								if(resObj.success) {
									resObj.success(pos, subResponse, textStatus, postData[category]);
								}								
							} else {
								if(resObj.error) {
									resObj.error(pos, subResponse, textStatus, postData[category]);
								}								
							}
						});
					} else {
						console.log("No " + resObj.key + " in response");
					}
				}
			});
			
			if (response.isSuccess()) {				
				$.each(requiredFields, function(i, field) {
					if (field && !response[field]) {
						console.log("No field: \"" + field + "\" in response");
						valid = false;
					}
				});
			} else {
				valid = false;
			}
			
			if (valid) {
				if(handler) {
					handler(response, textStatus, postData[category]);
				}				
			} else {
				console.log(response.toString());
				
				if(errorHandler) {
					errorHandler(response, textStatus, postData[category]);
				}
			}
		} else {
			var errorMsg = "No category: \"" + category + "\" in response";
			console.log(errorMsg);
			
			if(badResponseHandler) {
				badResponseHandler(errorMsg, textStatus, postData[category]);
			}
		}
		
		if(completeHandler) {
			completeHandler(textStatus, postData[category]);
		}		
	};	
	
	this.addResponseArray = function(key, conditionFunc, success, error) {
		responseArray.push({
			key : key,
			condition : conditionFunc,
			success : success,
			error : error
		});
		
		return thisObj;
	};

	this.getHandler = function() {
		return theHandler;
	};
	
	this.error = function (newErrorHandler) {
		errorHandler = newErrorHandler;
		return thisObj;
	};
	
	this.success = function (newSuccessHandler) {
		handler = newSuccessHandler;
		return thisObj;
	};
	
	this.complete = function (newCompleteHandler) {
		completeHandler = newCompleteHandler;
		return thisObj;
	};
	
	this.badResponseHandler = function (newBadResponseHandler) {
		badResponseHandler = newBadResponseHandler;
		return thisObj;
	};
	
	return this;
};var Application = function(id,container,titleText) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	var selected = false;
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	if(!this.frame) {
		this.frame = $("<div/>")
		.addClass("applicationContainer")
		.appendTo(container)
		.hide();
	}

	if(!this.title) {
		this.title = new TitleArea(titleText).appendTo(this.frame);
	}
	
	/****************************************************************************
	 * Event Listeners
	  ***************************************************************************/	
	eWolf.bind("select."+id,function(event,eventId) {
		if(id == eventId) {	
			self.doSelect();
		} else {
			self.doUnselect();
		}			
	});
	
	eWolf.bind("destroy."+id,function(event,eventId) {
		self.destroy();
	});		
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/	
	this.getFrame = function() {
		return self.frame;
	};
	
	this.getId = function() {
		return id;
	};
	
	this.isSelected = function() {
		return selected;
	};
	
	this.select = function() {
		eWolf.trigger("select",[id]);
		return self;
	};
	
	this.doSelect = function() {
		if(!selected) {
			self.frame.show(0);
			self.frame.animate({
				opacity : 1,
			}, 700, function() {
			});
			
			selected = true;
		}
		return self;
	};
	
	this.doUnselect = function() {
		if(selected) {
			self.frame.animate({
				opacity : 0,
			}, 300, function() {
				self.frame.hide(0);
			});
			
			selected = false;
			
			self.frame.stopAllYouTubePlayers();
		}	
		return self;
	};
	
	this.destroy = function() {
		eWolf.unbind("select."+id);
		eWolf.unbind("destroy."+id);
		self.frame.remove();
		eWolf.serverRequest.unregisterApp(id);
		delete self;
	};
	
	return this;
};VALIDATOR_IS_NOT_EMPTY = function(field) {
	return field.val() != "";
};

var FormValidator = function() {
	var self = this;
	
	var fields = {};
	var onSend = null;
	
	this.attachOnSend = function(newOnSend) {
		onSend = newOnSend;
		return self;
	};
	
	this.sendForm = function() {
		if(self.isValid(true) && onSend) {
			onSend();
		}
	};
	
	this.registerField = function (fieldId, field, itsErrorBox) {
		if(fieldId) {
			fields[fieldId] = {
				field	: field,
				error : itsErrorBox,
				lastCheckStatus : true,
				isVergin : true,
				isMarkedOK : false,
				validators : []
			};
			
			field.bind('input propertychange',function() {
				fields[fieldId].isVergin = false;
				self.isValid(false);
			});
			
			field.keyup(function(event) {
			    if(event.keyCode == 13) {
			    	self.sendForm();
			    	}
			});
		}
		
		return self;
	};
	
	this.addValidator = function (fieldId, validator, errorMessage) {
		if(fieldId && fields[fieldId]) {
			fields[fieldId].validators.push( {
				isValid : validator,
				errorMessage : errorMessage
			});
		}
		
		return self;
	};
	
	this.isValid = function(markOK) {
		var allValid = true;
		
		$.each(fields, function(fieldId, f) {
			if(!markOK && f.isVergin) {
				return true;
			}
			
			var fieldValid = true;
			var fieldErrorMessage = "";
			
			$.each(f.validators, function(j, validator) {
				var fieldValidatorValid = validator.isValid(f.field);
				
				if(fieldValidatorValid == false) {
					fieldErrorMessage = validator.errorMessage;
					fieldValid = false;
					allValid = false;
				}
				
				return fieldValidatorValid;
			});
			
			if(fieldValid) {
				if(f.lastCheckStatus == false || (markOK && !f.isMarkedOK)) {
					f.isMarkedOK = markOK;
					
					f.field.animate({
						"background-color" : markOK ? "#bddec0" : "#ddd" 
					},300, function() {
						if(!markOK) {
							f.field.css("background-color","");
						}						
					});
				}
				
				if(f.lastCheckStatus == false) {
					f.error.animate({
						"opacity" : "0"
					},300, function() {
						f.error.html("");
					});
				}
			} else {
				if(f.lastCheckStatus == true) {
					f.isMarkedOK = false;
					
					f.field.animate({
						"background-color" : "#debdbd"
					},300);	
				}
				
				if(f.error.html() != fieldErrorMessage) {
					f.error.animate({
						"opacity" : "0"
					},150, function() {
						f.error.html(fieldErrorMessage);
						
						f.error.animate({
							"opacity" : "1"
						},300);
					});
				}
			}
			
			f.lastCheckStatus = fieldValid;
		});		
		
		return allValid;
	};
	
	this.clearField = function (fieldId) {
		if(fieldId && fields[fieldId] &&
				!fields[fieldId].isVergin &&
					(		fields[fieldId].isMarkedOK || 
							fields[fieldId].lastCheckStatus == false)) {
			fields[fieldId].field.animate({
				"background-color" : "#ddd" 
			},150, function() {
				fields[fieldId].field.css("background-color","");				
			});
		}
		
		return self;
	};
	
	this.clearAllFields = function () {
		$.each(fields, function(fieldId, f) {
			self.clearField(fieldId);
		});
		return self;
	};
	
	return this;
};
var PopUp = function(frame, activator, leftOffset, bottomOffset, width) {
	var self = this;
	
	var pos = $(activator).position();

	// .outerWidth() takes into account border and padding.
	if(!width) {
		width = $(activator).outerWidth() - 26;
	}	
	var height = $(activator).outerHeight();
	
	var leftMargin = parseInt($(activator).css("margin-left"));

	//show the menu directly over the placeholder
	this.frame = $("<div/>").css({
		position : "fixed",
		top : (pos.top + $(frame).offset().top + height + bottomOffset) + "px",
		left : (pos.left + $(frame).offset().left + leftOffset + leftMargin) + "px",
		width : width,
		"border": "1px solid #999",
		"background-color" : "white",
		"z-index" : "1000"
	}).appendTo(document.body).hide();
	
	function clickFunc() {
		if(! self.frame.is(":hover")) {
			self.destroy();
		}
	};	
	
	this.destroy = function () {
		self.frame.hide(500,function() {
			self.frame.remove();
		});
		 $(document).unbind("click",clickFunc);
		 delete self;
	};
	
	this.start = function() {
		self.frame.show(500, function() {
			$(document).bind("click",clickFunc);
		});
		
		return self;
	};
	
	return this;
};var AddMembersToWolfpack = function(fatherID,wolfpack, 
		existingMemebers,	onFinish) {
	var self = this;
	this.frame = $("<span/>");
	
	madeChanges = false;	

	addMembersQuery = new FriendsQueryTagList(400).appendTo(this.frame);
	
	$.each(existingMemebers, function(i, item) {
		addMembersQuery.addTagByQuery(item,false);
	});
	
	applyBtn = $("<input/>").attr({
		"type": "button",
		"value": "Apply"
	}).appendTo(this.frame);
	
	cancelBtn = $("<input/>").attr({
		"type": "button",
		"value": "Cancel"
	}).appendTo(this.frame);
	
	errorMessage = $("<div/>").addClass("errorArea").appendTo(this.frame);
	
	responseHandler = new ResponseHandler("addWolfpackMember",[],null);
	
	this.apply = function() {
		var itemsToAdd = addMembersQuery.tagList.match({removable:true});
		
		if(!addMembersQuery.isMissingField(true, "Please add new members...")) {
			applyBtn.hide(200);
			cancelBtn.hide(200);
			
			errorMessage.html("");
			
			eWolf.serverRequest.request(fatherID,{
				addWolfpackMember: {
					wolfpackNames: [wolfpack],
					userIDs: itemsToAdd.getData()
				}
			},responseHandler.getHandler());
		}		
		
		return self;
	};
	
	this.cancel = function() {
		if(onFinish != null) {
			onFinish();
		}
		
		self.frame.remove();
		
		if(madeChanges) {
			madeChanges = false;
			eWolf.serverRequest.requestAll(fatherID,true);
		}
		
		delete self;
	};
	
	this.success = function(data, textStatus, postData) {
		madeChanges = true;
		self.cancel();
	};
	
	this.wolfpackError = function(pos, response, textStatus, postData) {
		errorMessage.append(response.toString()+"<br>");
	};
	
	this.userSuccess = function(pos, response, textStatus, postData) {
		var itemID = postData.userIDs[pos];
		var item = addMembersQuery.tagList.match({id:itemID});
		
		madeChanges = true;
		item.unremovable().markOK();			
	};
	
	this.userError = function(pos, response, textStatus, postData) {
		var itemID = postData.userIDs[pos];
		var item = addMembersQuery.tagList.match({id:itemID});
		
		var errorMsg = "Failed to add: " + itemID +
					" with error: " + response.toString();
		errorMessage.append(errorMsg+"<br>");
			
		item.markError(errorMsg);
	};
	
	this.error = function(response, textStatus, postData) {		
		if(!response.isSuccess() && !response.isGeneralError()) {
			errorMessage.append("Unknown error...<br>");
		}
	};
	
	this.complete = function (textStatus, postData) {
		if(madeChanges) {
			madeChanges = false;
			eWolf.serverRequest.requestAll(fatherID,true);
		}
		
		applyBtn.show(200);
		cancelBtn.show(200);
	};
	
	applyBtn.click(this.apply);	
	cancelBtn.click(this.cancel);
	
	responseHandler
		.success(this.success)	
		.error(this.error)	
		.complete(this.complete)
		.addResponseArray("wolfpacksResult",
				RESPONSE_ARRAY_CONDITION_GENRAL_ERROR,
				null,this.wolfpackError)
		.addResponseArray("usersResult",
				RESPONSE_ARRAY_CONDITION_GENRAL_ERROR,
				this.userSuccess, this.userError);
	
	return this;
};var AddToWolfpack = function(id, userID, frame, activator, 
		packsAlreadyIn, leftOffset, bottomOffset, width) {
	var self = this;
	PopUp.call(this,frame,activator, leftOffset, bottomOffset, width);
	
	var packList = $("<ul/>").attr({
		"class": "packListSelect"
	}).appendTo(this.frame);	

	$.each(eWolf.wolfpacks.wolfpacksArray,function(i,pack) {
		var box = $("<input/>").attr({
			"value" : pack,
			"type": "checkbox"
		});
		
		if(packsAlreadyIn.indexOf(pack) >= 0) {
			box.attr({
				"checked" : "checked",
				"disabled" : true
			});
			box.data("isMember",true);
		} else {
			box.data("isMember",false);
		}

		$("<li/>").attr({
			"class": "packListSelectItem"
		}).append(box).append(pack).appendTo(packList);
	});
	
	var createItem = $("<li/>").attr({
		"class": "packListSelectItem"
	}).css({
		"margin-top": "5px"
	}).appendTo(packList);
	
	var createLink = $("<span/>").attr({
		"class": "aLink createLink"
	});
	
	createLink.append("+ new wolfpack").appendTo(createItem).click(function() {
		var newPackItem = $("<li/>").attr({
			"class": "packListSelectItem"
		});		

		
		var newPack = $("<input/>").attr({
			"type":"text",
			"class": "newWolfpackInput"
		}).css({
			"width" : (parseInt(createLink.css("width")) - 5) + "px"
		});
		
		var itsCheckbox = $("<input/>").attr({
			"type": "checkbox",
			"disabled" : true
		}).data({
			"isNew" : true,
			"itsInput" : newPack
		}).appendTo(newPackItem);
		
		newPack.appendTo(newPackItem);
		newPack.keyup(function(event) {
		    if(newPack.val() != "") {
		    	itsCheckbox.attr("checked",true);
		    	itsCheckbox.removeAttr("disabled");
		    } else {
		    	itsCheckbox.attr({
		    		"checked" : false,
		    		"disabled" : true
		    	});
		    }
		});
			
		createItem.before(newPackItem);
		
		window.setTimeout(function () {
			newPack.focus();
		}, 0);	
	});
	
	function trimSpaces(s) {
		s = s.replace(/(^\s*)|(\s*$)/gi,"");
		s = s.replace(/[ ]{2,}/gi," ");
		s = s.replace(/\n /,"\n");
		return s;
	}
	
	$("<hr/>").css({
		"margin":"0"
	}).appendTo(this.frame);
	
	var applyBtn = $("<span/>").attr({
		"class": "aLink applyLink"
	}).append("Apply").appendTo(this.frame);
	
	this.getSelection = function () {
		var result = {
			add : [],
			create : [],
			remove : []	
		};
	
		$.each(packList.find("input"),function(i,item) {
			var itsBox = $(item);
	
			if(itsBox.is(':checked') == true) {
				if(itsBox.data("isMember") != true) {
					if(itsBox.data("isNew") == true) {
						var packName = trimSpaces(itsBox.data("itsInput").val());
						result.add.push(packName);
						result.create.push(packName);
					} else {
						result.add.push(itsBox.attr("value"));
					}		
				}
			} else {
				if(itsBox.data("isMember") == true) {
					result.remove.push(itsBox.attr("value"));
				}
			}
		});
		
		return result;	
	};
	
	this.addToAllWolfpacks = function (wolfpacks) {
		if(wolfpacks.length > 0) {
			var response = new ResponseHandler("addWolfpackMember",[],null);
			
			response.complete(function (textStatus, postData) {
				eWolf.trigger("refresh",[id]);
			});			
			
			eWolf.serverRequest.request(id,{
				addWolfpackMember: {
					wolfpackNames: wolfpacks,
					userIDs: [userID]
				}
			},response.getHandler());
		}
	};
	
	this.apply = function() {
		result = self.getSelection();
		
		self.destroy();
		
		eWolf.wolfpacks.createWolfpacks(result.create, function () {
			self.addToAllWolfpacks(result.add);
		});
	};
	
	applyBtn.click(this.apply);
	
	this.start();
		
	return this;
};var CommaSeperatedList = function(title) {
	var list = $("<span/>")
		.addClass("CommaSeperatedListItem")
		.append(title+": ")
		.hide();
	
	var items = null;
	var itemsArray = [];
	
	this.addItem = function (item,itemName) {
		if(items == null) {
			items = $("<span/>").appendTo(list);
			list.show();
		} else {
			items.append(", ");
		}
		
		items.append(item);
		itemsArray.push(itemName);
	};
	
	this.removeAll = function() {
		if(items != null) {
			items.remove();
			items = null;
			itemsArray = [];
			list.hide();
		}		
	};
	
	this.getList = function () {
		return list;
	};
	
	this.getItemNames = function () {
		return itemsArray;
	};
	
	this.show = function (speed) {
		if(items != null) {
			list.show(speed);
		}
	};
	
	this.hide = function (speed) {
		list.hide(speed);
	};
	
	return this;
};function CreateFileItemBox(name,type,size,file) {
	var box =  $("<div/>").css({
		"display": "inline-block"
	});
	
	var attachImage = $("<img/>").attr({
		"src" : "/Paperclip.png",
		"align" : "absmiddle",
		"vertical-align" : "middle"
	}).css({
		"margin-right" : "3px"
	}).appendTo(box);
	
	$("<span/>").css({
		"white-space" : "normal"
	}).append(name).appendTo(box);
	
	if(type) {
		$("<span/>").css({
			"text-align" : "right",
			"font-size" : "10px",
			"margin-left" : "5px"
		}).append("(" + type + ")").appendTo(box);
	}
	
	if(size) {
		var fileSize = 0;
	    if (size > 1024 * 1024) {
	    	fileSize = (Math.round(size * 100 / (1024 * 1024)) / 100).toString() + 'MB';
	    } else {
	    	fileSize = (Math.round(size * 100 / 1024) / 100).toString() + 'KB';
	    }
	    
	   if(!type) {
		   fileSize =  "(" + fileSize + ")";
	   }
		
		$("<span/>").css({
			"text-align" : "right",
			"font-size" : "10px",
			"margin-left" : "5px"
		}).append(fileSize).appendTo(box);
	}
	
	if(file && file.type.substring(0,5) == "image") {
		new ThumbnailImageFromFile(file,file.name,
				0.7,100,50,function(img) {
			attachImage.remove();
			img.attr({
				"align" : "absmiddle"
			}).css({
				"margin-right" : "3px"
			});
			box.prepend(img);
		});
	}
	
	return box;
}var CreateNewWolfpackLink = function() {
	var self = this;
	$.extend(this,CREATE_NEW_WOLFPACK_LINK_CONSTANTS);
	
	var link = $("<a/>").append("+ Create Wolfpack");	
	var li = $("<li/>").append(link).click(function() {
		var diag = $("<div/>").attr({
			"id" : "dialog-confirm",
			"title" : "Create a new wolfpack"
		}).addClass("DialogClass");
		
		var line = $("<p/>").append("New wolfpack name: ").appendTo(diag);
		
		var query = $("<input/>").attr({
			"type": "text",
			"placeholder": "Wolfpack name"
		}).css({
			"min-width" : 200
		}).appendTo(line);		
		
		var errorBox = $("<span/>").addClass("errorArea").appendTo(diag);
		
		var formValidator = new FormValidator()
					.registerField(self.QUERY_ID, query, errorBox)
					.attachOnSend(function() {
							eWolf.wolfpacks.createWolfpacks([query.val()], null);			
							diag.dialog( "close" );
						})
					.addValidator(self.QUERY_ID, VALIDATOR_IS_NOT_EMPTY
							, " * Please enter a wolfpack name")
					.addValidator(self.QUERY_ID, function(field) {
							return $.inArray(field.val(),eWolf.wolfpacks.wolfpacksArray) == -1;
						}, " * Wolfpack with that name already exist");
		
		diag.dialog({
			resizable: true,
			modal: true,
			width: 550,
			buttons: {
				"Create": formValidator.sendForm,
				Cancel: function() {
					$( this ).dialog( "close" );
				}
			}
		});
	});
	
	return li;
};DATE_FORMAT = "dd/MM/yyyy (HH:mm)";

function CreateTimestampBox(timestamp) {	
	return $("<span/>").addClass("timestampBox")
		.append(new Date(timestamp).toString(DATE_FORMAT));
}function CreateUserBox(id,name,showID) {
	if(id == null) {
		return null;
	}
	
	var link = $("<a/>").attr({
		"style": "width:1%;",
		"class": "selectableBox selectableBoxHovered",
	}).click(function() {
			eWolf.trigger("search",[id,name]);
	});
	
	var nameBox = $("<span/>").appendTo(link);
	
	var idBox = null;
	
	if(showID) {
		idBox = $("<span/>")
			.addClass("idBox")
			.appendTo(link);
		
		nameBox.addClass("selectableBoxHovered");
		link.removeClass("selectableBoxHovered");
	}
	
	function fillInformation() {
		nameBox.attr({
			"title": id
		}).text(name ? name : id);
		
		if(idBox) {
			idBox.html(id);
		}		
	}
	
	if (!name) {
		var fullDescID = eWolf.members.getUserFromFullDescription(id);
		
		if(fullDescID) {
			id = fullDescID;
		}
		
		name = eWolf.members.getUserName(id);

		if(name) {
			fillInformation();
		}
	}
	
	fillInformation();	

	return link;
}function CreateWolfpackBox(name) {
	var packAppID = eWolf.wolfpacks.getWolfpackAppID(name);
	
	return $("<span/>").attr({
		"style": "width:1%;",
		"class": "selectableBox selectableBoxHovered"
	}).text(name).click(function() {
		eWolf.selectApp(packAppID);
	});
}var FilesBox = function(uploaderArea) {
	var self = this;
	
	var fileselect;
	var filedrag = null;
	var filelist = null;
	var errorBox = null;
	
	// file unique ID
	var UID = 0;
	
	if (new XMLHttpRequest().upload) {
		fileselect = $("<input/>").attr({
			"type" : "file",
			"id" : "fileselect",
			"name" : "fileselect[]",
			"multiple" : "multiple"
		}).appendTo(uploaderArea);

		filedrag = $("<div/>")
			.addClass("fileDragBox")
			.append("drop files here")
			.appendTo(uploaderArea);

		filelist = new TagList(true,null,true).appendTo(uploaderArea);

		fileselect[0].addEventListener("change", FileSelectHandler, false);

		// file drop
		filedrag[0].addEventListener("dragover", FileDragHover, false);
		filedrag[0].addEventListener("dragleave", FileDragHover, false);
		filedrag[0].addEventListener("drop", FileDropHandler, false);		
		
		errorBox = $("<div/>")
			.addClass("errorArea")
			.appendTo(uploaderArea);
	}
	
	this.addFiles = function (files) {
		if(!files) {
			return;
		}
		
		// process all File objects
		var emptyFile = false;
		
		for ( var i = 0, f; f = files[i]; i++) {
			if(f.size != 0) {
				var itemBox = CreateFileItemBox(f.name,f.type,f.size,f);
				var thisUID = UID;
				UID += 1;
				filelist.addTag(thisUID, f, itemBox, true);
			} else {
				emptyFile = true;
			}
		}
		
		if(emptyFile) {
			errorBox.html("Can't upload an empty file or a folder.");
		} else {
			errorBox.html("");
		}
	};
	
	function FileDragHover(e) {
		e.stopPropagation();
		e.preventDefault();
		
		if(e.type == "dragover") {
			filedrag.addClass("fileDragBoxHover");
		} else {
			filedrag.removeClass("fileDragBoxHover");
		}
	}
	
	function FileDropHandler(e) {
		// cancel event and hover styling
		FileDragHover(e);
		self.addFiles(e.dataTransfer.files);
	}

	function FileSelectHandler(e) {
		self.addFiles(e.target.files);
	}
		
	this.markError = function (id) {
		filelist.match({id:id})
			.removeProgressBar()
			.markError("There was an error attempting to upload the file.");
	};
	
	this.markOK = function (id,fileObj) {
		filelist.match({id:id})
			.removeProgressBar()
			.markOK()
			.setData(fileObj);
	};

	this.getUploadedFiles = function() {
		if(!filelist) {
			return [];
		} else {
			return filelist.match({markedOK:true}).getData();
		}		
	};
	
	this.getCallbackAddress = function (wolfpackName, fileName, fileType) {
		return  "/sfsupload?" + "wolfpackName=" + wolfpackName
			+ "&fileName=" + fileName
			+ "&contentType=" + fileType;
	};
	
	this.initProgress = function (id) {
		filelist.initProgressBar(id);
	};
	
	this.showProgress = function (id,percentComplete) {
		filelist.setProgress(id,percentComplete);
	};
	
	this.uploadFileViaXML = function (id,file,wolfpackName,
			onSuccess,onError,onComplete) {
		var xhr = new XMLHttpRequest();
		
		self.initProgress(id);
		
		/* event listners */
		xhr.upload.addEventListener("progress", function(evt) {			
			if (evt.lengthComputable) {
				var percentComplete = Math.round(evt.loaded * 100
						/ evt.total);
				self.showProgress(id, percentComplete);
			}
		}, false);
		
		xhr.addEventListener("load", function (evt) {
			var response = JSON.parse(xhr.responseText);
			if(response.result != RESPONSE_RESULT.SUCCESS) {
				onError(response);
			} else {
				onSuccess(response);
			}
			
			onComplete(response);
		}, false);
		
		xhr.addEventListener("error", function (evt) {
			var response = JSON.parse(xhr.responseText);
			onError(response);
			onComplete(response);
		}, false);
		
		xhr.addEventListener("abort", function (evt) {
			var response = JSON.parse(xhr.responseText);
			onError(response);
			onComplete(response);
		}, false);
		
		filelist.setOnRemoveTag(id, function(id) {
			xhr.abort();
		});

		xhr.open("POST",self.getCallbackAddress(wolfpackName,
				file.name, file.type));
		xhr.send(file);
	};
	
	this.uploadFile = this.uploadFileViaXML;
	
	this.uploadAllFiles = function(wolfpackName,onComplete) {
		if(!filelist || filelist.match({markedOK:false}).isEmpty()) {
			onComplete(true,[]);
			return this;
		}
		
		function onCompleteOneFile() {
			if(filelist.match({markedOK:false,markedError:false}).isEmpty()) {
				var success = false;
				if(filelist.match({markedError:true}).isEmpty()) {
					success = true;
				}
				
				return onComplete(success,self.getUploadedFiles());
			}
		}
		
		filelist.match({markedOK:false}).each(function(id,file) {			
			self.uploadFile(id, file, wolfpackName, function(response) {
				self.markOK(id,{
					filename: file.name,
					contentType: file.type,
					size: file.size,
					path: response.path
				});
			},function(response) {
				self.markError(id);
			},onCompleteOneFile);
		});
		
		return this;
	};
	
	return this;
};
var FunctionsArea = function () {
	var self = this;
	
	this.frame = $("<div/>");
	
	var functions = {};
	
	this.appendTo = function (container) {
		self.frame.appendTo(container);
		return self;
	};
	
	this.addFunction = function (functionName,functionOp, hide) {
		if(functions[functionName] == null) {
			functions[functionName] = $("<input/>").attr({
				"type": "button",
				"value": functionName
			}).click(functionOp).appendTo(self.frame);
			
			if(hide) {
				functions[functionName].hide();
			}
		}
		
		return self;
	};
	
	this.removeFunction = function (functionName) {
		if(functions[functionName] != null) {
			functions[functionName].remove();
			functions[functionName] = null;
		}
		
		return self;
	};
	
	this.hideFunction = function (functionName) {
		if(functions[functionName] != null) {
			functions[functionName].hide(200);
		}
		
		return self;
	};
	
	this.showFunction = function (functionName) {
		if(functions[functionName] != null) {
			functions[functionName].show(200);
		}
		
		return self;
	};
	
	this.hideAll = function () {
		for(var functionName in functions) {
			self.hideFunction(functionName);
		}
		
		return self;
	};
	
	this.showAll = function () {
		for(var functionName in functions) {
			self.showFunction(functionName);
		}
		
		return self;
	};
	
	return this;
};var Loading = function(indicator) {
	var loadingCount = 0;
	
	function startLoading() {
		loadingCount++;
		indicator.spin(spinnerOpts);
		indicator.show(200);
	}
	
	function stopLoading() {
		indicator.hide(200);
		indicator.data('spinner').stop();
		loadingCount--;
	}
	
	eWolf.bind("loading",startLoading);	
	eWolf.bind("loadingEnd",stopLoading);


	this.listenToEvent = function(eventStart,eventEnd) {
		eWolf.bind(eventStart,startLoading);	
		eWolf.bind(eventEnd,stopLoading);
		return this;
	};
	
	this.stopListenToEvent = function(eventStart,eventEnd) {
		eWolf.unbind(eventStart,startLoading);	
		eWolf.unbind(eventEnd,stopLoading);
		return this;
	};
	
	return this;
};

var spinnerOpts = {
  lines: 10, // The number of lines to draw
  length: 4, // The length of each line
  width: 2, // The line thickness
  radius: 3, // The radius of the inner circle
  rotate: 0, // The rotation offset
  color: '#fff', // #rgb or #rrggbb
  speed: 0.8, // Rounds per second
  trail: 60, // Afterglow percentage
  shadow: false, // Whether to render a shadow
  hwaccel: false, // Whether to use hardware acceleration
  className: 'spinner', // The CSS class to assign to the spinner
  zIndex: 2e9, // The z-index (defaults to 2000000000)
  top: 0, // Top position relative to parent in px
  left: 0 // Left position relative to parent in px
};

$.fn.spin = function(opts) {
	this.each(function() {
		var $this = $(this), data = $this.data();

		if (data.spinner) {
			data.spinner.stop();
			delete data.spinner;
		}
		if (opts !== false) {
			data.spinner = new Spinner($.extend({
				color : $this.css('color')
			}, opts)).spin(this);
		}
	});
	return this;
};var Logout = function(id,text,container) {
	var self = this;	

	this.frame =$("<div/>").attr({
		"class": "logoutLink aLink"
	})	.text(text)
		.appendTo(container)
		.click(function() {
			$(window).unbind('hashchange');
			window.location.hash = "";
			
			self.commitLogout();			
		});
	
	function onLogout(appID) {
		document.location.reload(true);
	}
	
	this.commitLogout = function () {
		eWolf.serverRequest.request(id,{
				logout : {}
			}, null, onLogout);
	};
	
	this.destroy = function() {
		if(self.frame != null) {
			self.frame.remove();
			self.frame = null;
			delete self;
		}
	};
	
	return this;
};

var Notification = function(context, onItem, aboveItem, rightToItem) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	var currentNumber = 0;
		
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.notification = $("<span/>")
											.addClass("notification")
											.appendTo(context);
	
	if(onItem) {
		var pos = $(onItem).position(),
				width = $(onItem).outerWidth(),
				leftMargin = parseInt($(onItem).css("margin-left")),
				topMargin = parseInt($(onItem).css("margin-top"));
		
		self.notification.addClass("onItemNotification")
			.css({
		    top : (pos.top + topMargin - aboveItem) + "px",
		    left : (pos.left + width + leftMargin - 18 + rightToItem) + "px",
		});
	}
	
	this.notification.hide();
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/	
	this.setCounter = function (number) {
		if(self.notification) {
			if(number > 0) {
				self.notification.html(number);
				
				if(currentNumber <= 0) {
					self.notification.show(300);
				}
			} else {
				if(currentNumber > 0) {
					self.notification.hide(300);
				}				
			}
		}
		
		currentNumber = number;
		
		return self;
	};
	
	this.getCounter = function() {
		return currentNumber;
	};
	
	return this;
};
var PendingApprovalList = function(frame,activator, users, 
		leftOffset, bottomOffset, width) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	
	/****************************************************************************
	 * Base Class
	  ***************************************************************************/
	PopUp.call(this,frame,activator, leftOffset, bottomOffset, width);
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.context = $("<div/>").css({
		"padding" : "5px",
	}).appendTo(this.frame);
	
	//this.
	
	$.each(users, function(i, id) {
		self.context.append(CreateUserBox(id));
	});
		
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/
	this.start();
	
	return this;
};var PendingRequests = function (insideContext) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	
	var approved = [],
			approveMe = [],
			pendingApproval = [],
			requestApproval = [];
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.context = $("<div/>")
				.addClass("title-bar")
				.appendTo(insideContext);
	
	var pendingRequestImage = $("<img/>").attr({
		"src": "user-add.png",
	})	.css({
		"width" : "28px",
		"height" : "28px"
	})	.addClass("pendingNotificationImage")
			.appendTo(this.context);
	
	var blockedImage = $("<img/>").attr({
		"src": "user-blocking.png",
	})	.css({
		"width" : "32px",
		"height" : "28px"
	})	.addClass("pendingNotificationImage")
			.appendTo(this.context);
	
	var pendingCount = new Notification(this.context, pendingRequestImage, 6, 1)
					.setCounter(0);
	
	var blockingCount = new Notification(this.context, blockedImage, 6, 6)
					.setCounter(0);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/
	this.handleApproved = function(response, textStatus, postData) {
		approved = [];
		$.each(response.membersList, function(i,userObj){
			eWolf.members.addKnownUsers(userObj.id,userObj.name);
			approved.push(userObj.id);
		});
	};
	
	this.handleApprovedMe = function(response, textStatus, postData) {
		approveMe = [];
		$.each(response.membersList, function(i,userObj){
			eWolf.members.addKnownUsers(userObj.id,userObj.name);
			approveMe.push(userObj.id);
		});
	};
	
	this.updateNotifications = function () {
		var res = compareMissingInArrays(approved, approveMe);
		
		pendingApproval = res.missingIn1;
		requestApproval = res.missingIn2;
		
		var pendingApprovalCount = pendingApproval.length,
				requestApprovalCount = requestApproval.length;
		
		if(pendingApprovalCount > 0 && pendingCount.getCounter() <= 0) {
			pendingRequestImage.animate({
				opacity : 0.7
			}, 300);
		} else if(pendingApprovalCount <= 0 && pendingCount.getCounter() > 0){
			pendingRequestImage.animate({
				opacity : 0.2
			}, 300);
		}
		
		if(requestApprovalCount > 0 && blockingCount.getCounter() <= 0) {
			blockedImage.animate({
				opacity : 0.7
			}, 300);
		} else if(requestApprovalCount <= 0 && blockingCount.getCounter() > 0){
			blockedImage.animate({
				opacity : 0.2
			}, 300);
		}

		pendingCount.setCounter(pendingApprovalCount);
		blockingCount.setCounter(requestApprovalCount);
	};
	
	this.appendTo = function(somthing) {
		if(self.context) {
			self.context.appendTo(somthing);
		}
	};
	
	eWolf.serverRequest.registerRequest(eWolf.APPROVED_MEMBERS_REQUEST_NAME,
			function() {
				var result = {};
				result[eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS1] = {
						wolfpackName : eWolf.APPROVED_WOLFPACK_NAME
				};
				
				result[eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS2] = {
						wolfpackName : eWolf.APPROVED_ME_WOLFPACK_NAME
				};
				
				return result;
			});
	
	eWolf.serverRequest.bindRequest(eWolf.APPROVED_MEMBERS_REQUEST_NAME);
	
	eWolf.serverRequest.registerHandler(eWolf.APPROVED_MEMBERS_REQUEST_NAME,
			new ResponseHandler(
					eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS1,
					["membersList"],
				this.handleApproved).getHandler());
	
	eWolf.serverRequest.registerHandler(eWolf.APPROVED_MEMBERS_REQUEST_NAME,
			new ResponseHandler(
					eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS2,
					["membersList"],
				this.handleApprovedMe).getHandler());
	
	eWolf.serverRequest.addOnComplete(null,function(appID, response, status) {
		self.updateNotifications();
	});
	
	pendingRequestImage.click(function() {
		if(pendingApproval.length > 0) {
			new PendingApprovalList(document.body, pendingRequestImage,
					 pendingApproval, -7, 8, 200);
		}		
	});
	
	blockedImage.click(function() {
		if(requestApproval.length > 0) {
			new PendingApprovalList(document.body, blockedImage,
					 requestApproval, -7, 8, 200);
		}		
	});
	
	return this;
};

function compareMissingInArrays (arr1, arr2) {
	arr1.sort();
	arr2.sort();
	
	var len1 = arr1.length,
			len2 = arr2.length,
			i = 0,
			j = 0,
			missingIn1 = [],
			missingIn2 = [];
	
	while(i < len1 && j < len2) {
		if(arr1[i] == arr2[j]) {
			i++;
			j++;
		} else if(arr1[i] < arr2[j]) {
			missingIn2.push(arr1[i]);
			i++;
		} else {
			missingIn1.push(arr2[j]);
			j++;
		}
	}
	
	if(i < len1) {
		missingIn2 = missingIn2.concat(arr1.slice(i,len1));
	}
	
	if(j < len2) {
		missingIn1 = missingIn1.concat(arr2.slice(j,len2));
	}
	
	return {
		missingIn1 : missingIn1,
		missingIn2 : missingIn2
	};
}var QueryTagList = function(minWidth,queryPlaceHolder,availableQueries,
		allowMultipleDestinations,commitQuery) {
	var self = this;
	
	this.frame = $("<div/>").attr("class","seachListClass");	
	var queryBox = $("<div/>").appendTo(this.frame);
	
	var query = $("<input/>").attr({
		"type": "text",
		"placeholder": queryPlaceHolder
	}).css({
		"min-width" : minWidth
	}).appendTo(queryBox);
	
	var addBtn = $("<input/>").attr({
		"type": "button",
		"value": "Add"
	}).click(function() {
		self.addTagByQuery(query.val(),true);
	}).appendTo(queryBox).hide();
	
	var errorBox = $("<div/>").addClass("errorArea").appendTo(queryBox).hide();
	
	query.autocomplete({
		source: availableQueries,
		select: onSelectSendTo
	}).keyup(function(event) {
	    if(event.keyCode == 13 && query.val() != "") {
	    	self.addTagByQuery(query.val(),true);   	
	    } else {
	    	updateQuery();
	    	}	    
	});
	
	query.bind('input propertychange',function() {
		if(query.val() == "") {
			addBtn.hide(200);
		} else {
			addBtn.show(200);
		}
	});
	
	function onSelectSendTo(event,ui) {		
		self.addTagByQuery(ui.item.label,true);
		return false;
	}
	
	function updateQuery (id) {			
		if(!allowMultipleDestinations) {
			if(! self.tagList.isEmpty()) {
				queryBox.hide();
			} else {
				queryBox.show();
			}
		}
	}
	
	this.tagList = new TagList(false,updateQuery).appendTo(this.frame);
		
	this.addTagByQuery = function(thisQuery,removable) {
		var res = commitQuery(thisQuery);
		// sould return:	res.term and res.display
		
		if(res == null) {
			return false;
		}
		
		if(self.tagList.addTag(res.term,res.term,res.display,removable)) {
				query.val("");
				addBtn.hide(200);
    		updateQuery();
    		self.isMissingField(false);
    		return true;
		} else {
			return false;
		}		
	};
	
	this.isMissingField = function (showError, errorMessage) {
		var fieldEmpty = self.tagList.match({removable:true}).isEmpty();
		
		errorBox.animate({
			"opacity" : "0"
		},500,function() {
			if(fieldEmpty && showError) {
				query.focus();
				errorBox.html(errorMessage);
				errorBox.show();
				
				errorBox.animate({
					"opacity" : "1"
				},1000);
				
				query.animate({
					"background-color" : "#debdbd"
				},1000);
			}
			
			if(!fieldEmpty) {
				query.animate({
					"background-color" : "#ddd"
				},1000);
				
				errorBox.hide();
			}
		});
		
		return fieldEmpty;
	};
	
	this.appendTo = function(someFrame) {
		this.frame.appendTo(someFrame);
		return this;
	};
	
	this.focus = function () {
		query.focus();
		return this;
	};
	
	return this;
};

var FriendsQueryTagList = function (minWidth) {
	function sendToFuncReplace(query) {
		var id = eWolf.members.getUserFromFullDescription(query);
		var name = null;
		
		if(id) {
			name = eWolf.members.getUserName(id);
		} else {
			id = query;
		}
		
		return {
			term: id,
			display: CreateUserBox(id,name)
		};
	}
	
	return new QueryTagList(minWidth,"Type friend name or ID...",
			eWolf.members.knownUsersFullDescriptionArray,true,sendToFuncReplace);
};

var WolfpackQueryTagList = function (minWidth) {
	function sendToFuncReplace(pack) {
		var idx = eWolf.wolfpacks.wolfpacksArray.indexOf(pack);
		if(idx != -1) {
			return {
				term: pack,
				display: CreateWolfpackBox(pack)
			};
		} else {
			return null;
		}		
	}
	
	return new QueryTagList(minWidth,"Type wolfpack name...",
			eWolf.wolfpacks.wolfpacksArray,false,sendToFuncReplace);
};var SearchBar = function(menu,applicationFrame,container) {
	var self = this;
	$.extend(this,SEARCHBAR_CONSTANTS);
	
	//var menuList = menu.createNewMenuList(this.SEARCH_MENU_ITEM_ID,"Search");
	var apps = new Object();
	var lastSearch = null;
	
	this.frame = $("<div/>")
		.addClass("title-bar")
		.appendTo(container);
	
	var query = $("<input/>").attr({
		"type" : "text",
		"placeholder" : "Search",
		"autocomplete" : "off",
		"spellcheck" : "false"
	}).css({
		"width" : "400px"
	}).autocomplete({
		source: eWolf.members.knownUsersFullDescriptionArray,
		select: onSelectAutocomplete
	}).appendTo(this.frame);
	
	eWolf.bind("foundNewUser",function(event,id,name,fullDescription) {
		query.autocomplete("destroy").autocomplete({
			source: eWolf.members.knownUsersFullDescriptionArray,
			select: onSelectAutocomplete
		});
	});
	
	function onSelectAutocomplete(event,ui) {
		self.search(ui.item.label);
		return false;
	}

	var searchBtn = $("<input/>").attr({
		"type" : "button",
		"value" : "Search"
	}).appendTo(this.frame).hide();
	
	function addSearchMenuItem(id,name) {
//		var tempName;
//		if(name == null) {
//			tempName = "Search: " + id;
//		} else {
//			tempName = name;
//		}
//		
		var searchedProfileAppKey = self.SEARCH_PROFILE_PREFIX + id;
		//menuList.addMenuItem(searchedProfileAppKey,tempName);
		apps[searchedProfileAppKey] = new Profile(searchedProfileAppKey,applicationFrame,id,name)
			.onReceiveName(function(newName) {
				//menuList.renameMenuItem(searchedProfileAppKey,newName);
			});	
		
		eWolf.selectApp(searchedProfileAppKey);
	};
	
	function removeSearchMenuItem(searchKey) {
		var searchedProfileAppKey = self.SEARCH_PROFILE_PREFIX + searchKey;
		
		if(apps[searchedProfileAppKey] != null) {
			apps[searchedProfileAppKey].destroy();
			delete apps[searchedProfileAppKey];
			apps[searchedProfileAppKey] = null;
			//menuList.removeMenuItem(searchedProfileAppKey);
		}
	}
	
	function removeLastSearch() {
		if(lastSearch != null) {
			removeSearchMenuItem(lastSearch);
			lastSearch = null;
		}
	}
	
	this.search = function (key,name) {
		if(key == null) {
			key = query.val();
		}
		
		if(name == "") {
			name == null;
		}
		
		if(key != null && key != "") {				
			if(!name) {
				name = eWolf.members.getUserName(key);
			}

			if(!name) {
				var fullDescID = eWolf.members.getUserFromFullDescription(key);
				
				if(fullDescID) {
					key = fullDescID;
					name = eWolf.members.getUserName(fullDescID);
				}
			}
			
			var searchedProfileAppKey = self.SEARCH_PROFILE_PREFIX + key;
			
			if(key == eWolf.profile.getID()) {
				eWolf.selectApp(eWolf.MYPROFILE_APP_ID);
			} else if(apps[searchedProfileAppKey] != null) {
				console.log("not deleted");
			} else {
				removeLastSearch();
				lastSearch = key;
				addSearchMenuItem(key,name);
			}			
		}
		
		return self;
	};
	
	searchBtn.click(function() {
		self.search(query.val());	
	});
	
	eWolf.bind("select",function(event,eventId) {
		var lastSearchedProfileAppKey = self.SEARCH_PROFILE_PREFIX + lastSearch;
		var lastSearchNewMailAppKey = NEWMAIL_CONSTANTS.NEWMAIL_APP_ID_PREFIX
			+ lastSearchedProfileAppKey;
		if(eventId != lastSearchedProfileAppKey && eventId != lastSearchNewMailAppKey) {
			removeLastSearch();
		}
	});
	
	query.bind('input propertychange',function() {
		 if(query.val() == "") {
	    	searchBtn.hide(200);
	    } else {
	    	searchBtn.show(200);
	    }
	});
	
	query.keyup(function(event){
	    if(event.keyCode == 13 && query.val() != ""){
//	    	if(event.shiftKey) {
//	    		addBtn.click();
//	    	} else {
//	    		searchBtn.click();
//	    	}
	    	
	    	searchBtn.click();
	    }   
	});
		
	
	eWolf.bind("search",function(event,key,name) {
		self.search(key,name);
	});
	
	return this;
};var Tag = function(id,onRemove,removable,multirow,withImage) {
	var box = $("<p/>").addClass("TagClass");
	
	if(!removable) {
		box.addClass("TagNonRemoveable");
	}
	
	if(!multirow) {
		box.addClass("TagNoMultiRow");
	}

	$("<div/>").addClass("TagDeleteClass")
		.append("&times;")
		.appendTo(box)
		.click(function() {
			box.remove();
		
			if(onRemove) {
				onRemove(id);
			}
		});
	
	if(withImage) {
		box.addClass("TagWithImage");
	}
	
	box.data("initProgressBar", function() {
		var progress = $("<div/>").appendTo(box);
		
		progress.progressbar({ disabled: true });
		progress.css({
			"width" : "100%",
			"height" : "100%",
			"class" : "ui-progressbar",
			"z-index" : "1"
		});
		
		progress.children("div").css({
			'background': '#001a00'
		});
		
		box.children(":not(.ui-progressbar)").css({
			"position" : "relative",
			"z-index" : "999"
		});
		
		box.data("setProgress", function (prec) {
			progress.progressbar({ value: prec });
		});
		
		box.data("removeProgressBar", function() {
			progress.remove();
		});
	});	
	
	return box;
};var TagList = function(multirow,onRemoveTag,withImages) {
	this.div = $("<div/>");
	
	this.appendTo = function(someFrame) {
		this.div.appendTo(someFrame);
		return this;
	};
	
	this.getTags = function(matches) {
		var selector = ".TagClass";
		
		if(matches != null) {
			if(matches.id != null) {
				selector += "[id=\"" + matches.id + "\"]";
			}
			
			if(matches.markedError == true) {
				selector += ".TagErrorClass";
			} else if(matches.markedError == false){
				selector += ":not(.TagErrorClass)";
			}
			
			if(matches.markedOK == true) {
				selector += ".TagOKClass";
			} else if(matches.markedOK == false){
				selector += ":not(.TagOKClass)";
			}
			
			if(matches.removable == true) {
				selector += ":not(.TagNonRemoveable)";
			} else if(matches.removable == false){
				selector += ".TagNonRemoveable";
			}
		}		
		
		return this.div.children(selector);
	};
	
	this.match = function(matches) {
		var tags = this.getTags(matches);
		
		return {
			each: function (applyThis) {
				tags.each(function(i, thisTag) {
					var tag = $(thisTag);
					applyThis(tag.attr("id"),tag.data("tagData"));
				});
				
				return this;
			},
			unremovable: function () {
				tags.addClass("TagNonRemoveable");
				return this;
			},
			removable: function () {
				tags.removeClass("TagNonRemoveable");
				return this;
			},			
			markError: function (error) {
				tags.addClass("TagErrorClass")
					.removeClass("TagOKClass")
					.attr("title",error);
				return this;
			},			
			unmarkError: function () {
				tags.removeClass("TagErrorClass")
					.attr("title",null);
				return this;
			},			
			markOK: function () {
				tags.addClass("TagOKClass")
					.removeClass("TagErrorClass")
					.attr("title","Successful");
				return this;
			},			
			unmarkOK: function () {
				tags.removeClass("TagOKClass")
					.attr("title",null);
				return this;
			},			
			unmark: function () {
				tags.removeClass("TagErrorClass")
					.removeClass("TagOKClass")
					.attr("title",null);
				return this;
			},			
			remove: function () {
				tags.remove();
				return this;
			},			
			initProgressBar: function () {
				tags.each(function(i, thisTag) {
					 $(thisTag).data("initProgressBar")();
				});
				
				return this;
			},			
			setProgress: function (prec) {
				tags.each(function(i, thisTag) {
					var func = $(thisTag).data("setProgress");
					if(func) {
						return func(prec);
					}
				});
				
				return this;
			},			
			removeProgressBar: function () {				
				tags.each(function(i, thisTag) {
					var func = $(thisTag).data("removeProgressBar");
					if(func) {
						return func();
					}
				});
				
				return this;					
			},			
			setOnRemoveTag: function(newOnRemove) {
				tags.data("onRemove",newOnRemove);
				return this;
			},			
			setData: function (tagData) {
				tags.data("tagData",tagData);
				return this;
			},
			count: function () {
				return tags.length;
			},
			isEmpty: function () {
				return tags.length == 0;
			},
			getData: function () {
				result = [];
				tags.each(function(i, thisTag) {
					result.push($(thisTag).data("tagData"));
				});
				
				return result;
			}
		};
	};
	
	this.addTag = function(id,tagData,tagText,removable) {
		if( this.match({id:id}).isEmpty()) {
			var newTagItem = new Tag(id,onRemoveTag,removable,
					multirow,withImages)
				.attr("id",id)
				.data("tagData",tagData)
				.append(tagText);
		
			this.div.append(newTagItem);
			
			return true;
		}				
		
		return false;
	};
	
	this.removeTag = function (id) {
		this.match({id:id}).remove();
		return this;
	};
		
	this.foreachTag = function (matches,applyThis) {
		if(applyThis == null) {
			applyThis =  matches;
			matches = null;
		}
		
		this.match(matches).each(applyThis);
		
		return this;
	};
	
	this.setTagUnremovable = function (id) {
		this.match({id:id}).unremovable();
		return this;
	};
	
	this.setTagRmovable = function (id) {
		this.match({id:id}).removable();
		return this;
	};
	
	this.markTagError = function (id,error) {
		this.match({id:id}).markError(error);
		return this;
	};	
	
	this.unmarkTagError = function (id) {
		this.match({id:id}).unmarkError();
		return this;
	};
	
	this.markTagOK = function (id) {
		this.match({id:id}).markOK();
		return this;
	};	
	
	this.unmarkTagOK = function (id) {
		this.match({id:id}).unmarkOK();
		return this;
	};
	
	this.isEmpty = function() {
		return this.match().isEmpty();
	};
	
	this.tagCount = function(matches) {
		return this.match(matches).count();
	};
	
	this.unmarkTags = function (matches) {
		this.match(matches).unmark();
		return this;
	};
	
	this.removeTags = function (matches) {
		this.match(matches).remove();
		return this;
	};
	
	this.initProgressBar = function (id) {
		this.match({id:id}).initProgressBar();
		return this;
	};
	
	this.setProgress = function (id, prec) {
		this.match({id:id}).setProgress(prec);
		return this;
	};
	
	this.removeProgressBar = function (id) {
		this.match({id:id}).removeProgressBar();		
		return this;
	};
	
	this.setOnRemoveTag = function(id,newOnRemove) {
		this.match({id:id}).setOnRemoveTag(newOnRemove);
		return this;
	};
	
	this.setTagData = function (id,tagData) {
		this.match({id:id}).setData(tagData);
		return this;
	};
	
	return this;
};var ThumbnailImage = function (src,altText,quality,maxWidth,maxHeight,onReady) {
	var image = new Image();
	image.src = src;

	image.onload = function() {
		var imageWidth = image.width,
			imageHeight = image.height;

		if (imageWidth > imageHeight) {
			if (imageWidth > maxWidth) {
				imageHeight *= maxWidth / imageWidth;
				imageWidth = maxWidth;
			}
		} else {
			if (imageHeight > maxHeight) {
				imageWidth *= maxHeight / imageHeight;
				imageHeight = maxHeight;
			}
		}

		var canvas = document.createElement('canvas');
		canvas.width = imageWidth;
		canvas.height = imageHeight;

		var ctx = canvas.getContext("2d");
		ctx.drawImage(image, 0, 0, imageWidth, imageHeight);

		var data;
		try {
			data = canvas.toDataURL("image/jpeg",quality);
		} catch(e) {
			data = src;
		}
		
		var result = $("<img/>").attr({
			"src": data,
			"alt" : altText
		}).css({
			"max-width" : maxWidth+"px",
			"max-height" : maxHeight+"px"
		});
		
		onReady(result);
	};
};

var ThumbnailImageFromFile = function(file,altText,quality,maxWidth,maxHeight,onReady) {
	var reader = new FileReader();

	reader.onloadend = function() {
		ThumbnailImage.call(this,reader.result,altText,quality,maxWidth,maxHeight,onReady);
	};

	reader.readAsDataURL(file);
};var TitleArea = function (title) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.frame = $("<div/>").attr("class","titleArea");
	
	var topPart = $("<div/>").appendTo(this.frame);
	var bottomPart = $("<div/>")
		.attr("class","titleBottomPart")
		.appendTo(this.frame);
	
	var table = $("<table/>").attr("class","titleTable").appendTo(topPart);
	
	var row = $("<tr>").appendTo(table);
	var titleTextArea = $("<td>")
		.addClass("titleTextArea").appendTo(row);
	var titleFunctionsArea = $("<td>")
		.attr("class","titleFunctionsArea").appendTo(row);
	
	var functions = new FunctionsArea().appendTo(titleFunctionsArea);
	
	var theTitle = $("<span/>").attr({
		"class" : "eWolfTitle"
	}).appendTo(titleTextArea);
	
	var titleExtraText = $("<span/>").appendTo(titleTextArea);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/	
	this.setTitle = function (newTitle) {
		if(newTitle != null) {
			theTitle.html(newTitle);
		}
		
		return self;
	};
	
	this.appendTo = function (container) {
		this.frame.appendTo(container);
		return self;
	};
	
	this.appendAtTitleTextArea = function (obj) {
		titleExtraText.append(obj);
		return self;
	};
	
	this.appendAtTitleFunctionsArea = function (obj) {
		titleFunctionsArea.append(obj);
		return self;
	};
	
	this.appendAtBottomPart = function (obj) {
		bottomPart.append(obj);
		return self;
	};
	
	this.addFunction = function (functionName,functionOp, hide) {
		functions.addFunction(functionName,functionOp, hide);
		return self;
	};
	
	this.removeFunction = function (functionName) {
		functions.removeFunction(functionName);
		return self;
	};
	
	this.hideFunction = function (functionName) {
		functions.hideFunction(functionName);		
		return self;
	};
	
	this.showFunction = function (functionName) {
		functions.showFunction(functionName);
		return self;
	};
	
	this.hideAll = function () {
		functions.hideAll();
		return self;
	};
	
	this.showAll = function () {
		functions.showAll();
		return self;
	};
	
	this.setTitle(title);
	
	return this;
};/**
 * Version: 1.0 Alpha-1 
 * Build Date: 13-Nov-2007
 * Copyright (c) 2006-2007, Coolite Inc. (http://www.coolite.com/). All rights reserved.
 * License: Licensed under The MIT License. See license.txt and http://www.datejs.com/license/. 
 * Website: http://www.datejs.com/ or http://www.coolite.com/datejs/
 */
Date.CultureInfo={name:"en-US",englishName:"English (United States)",nativeName:"English (United States)",dayNames:["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],abbreviatedDayNames:["Sun","Mon","Tue","Wed","Thu","Fri","Sat"],shortestDayNames:["Su","Mo","Tu","We","Th","Fr","Sa"],firstLetterDayNames:["S","M","T","W","T","F","S"],monthNames:["January","February","March","April","May","June","July","August","September","October","November","December"],abbreviatedMonthNames:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],amDesignator:"AM",pmDesignator:"PM",firstDayOfWeek:0,twoDigitYearMax:2029,dateElementOrder:"mdy",formatPatterns:{shortDate:"M/d/yyyy",longDate:"dddd, MMMM dd, yyyy",shortTime:"h:mm tt",longTime:"h:mm:ss tt",fullDateTime:"dddd, MMMM dd, yyyy h:mm:ss tt",sortableDateTime:"yyyy-MM-ddTHH:mm:ss",universalSortableDateTime:"yyyy-MM-dd HH:mm:ssZ",rfc1123:"ddd, dd MMM yyyy HH:mm:ss GMT",monthDay:"MMMM dd",yearMonth:"MMMM, yyyy"},regexPatterns:{jan:/^jan(uary)?/i,feb:/^feb(ruary)?/i,mar:/^mar(ch)?/i,apr:/^apr(il)?/i,may:/^may/i,jun:/^jun(e)?/i,jul:/^jul(y)?/i,aug:/^aug(ust)?/i,sep:/^sep(t(ember)?)?/i,oct:/^oct(ober)?/i,nov:/^nov(ember)?/i,dec:/^dec(ember)?/i,sun:/^su(n(day)?)?/i,mon:/^mo(n(day)?)?/i,tue:/^tu(e(s(day)?)?)?/i,wed:/^we(d(nesday)?)?/i,thu:/^th(u(r(s(day)?)?)?)?/i,fri:/^fr(i(day)?)?/i,sat:/^sa(t(urday)?)?/i,future:/^next/i,past:/^last|past|prev(ious)?/i,add:/^(\+|after|from)/i,subtract:/^(\-|before|ago)/i,yesterday:/^yesterday/i,today:/^t(oday)?/i,tomorrow:/^tomorrow/i,now:/^n(ow)?/i,millisecond:/^ms|milli(second)?s?/i,second:/^sec(ond)?s?/i,minute:/^min(ute)?s?/i,hour:/^h(ou)?rs?/i,week:/^w(ee)?k/i,month:/^m(o(nth)?s?)?/i,day:/^d(ays?)?/i,year:/^y((ea)?rs?)?/i,shortMeridian:/^(a|p)/i,longMeridian:/^(a\.?m?\.?|p\.?m?\.?)/i,timezone:/^((e(s|d)t|c(s|d)t|m(s|d)t|p(s|d)t)|((gmt)?\s*(\+|\-)\s*\d\d\d\d?)|gmt)/i,ordinalSuffix:/^\s*(st|nd|rd|th)/i,timeContext:/^\s*(\:|a|p)/i},abbreviatedTimeZoneStandard:{GMT:"-000",EST:"-0400",CST:"-0500",MST:"-0600",PST:"-0700"},abbreviatedTimeZoneDST:{GMT:"-000",EDT:"-0500",CDT:"-0600",MDT:"-0700",PDT:"-0800"}};
Date.getMonthNumberFromName=function(name){var n=Date.CultureInfo.monthNames,m=Date.CultureInfo.abbreviatedMonthNames,s=name.toLowerCase();for(var i=0;i<n.length;i++){if(n[i].toLowerCase()==s||m[i].toLowerCase()==s){return i;}}
return-1;};Date.getDayNumberFromName=function(name){var n=Date.CultureInfo.dayNames,m=Date.CultureInfo.abbreviatedDayNames,o=Date.CultureInfo.shortestDayNames,s=name.toLowerCase();for(var i=0;i<n.length;i++){if(n[i].toLowerCase()==s||m[i].toLowerCase()==s){return i;}}
return-1;};Date.isLeapYear=function(year){return(((year%4===0)&&(year%100!==0))||(year%400===0));};Date.getDaysInMonth=function(year,month){return[31,(Date.isLeapYear(year)?29:28),31,30,31,30,31,31,30,31,30,31][month];};Date.getTimezoneOffset=function(s,dst){return(dst||false)?Date.CultureInfo.abbreviatedTimeZoneDST[s.toUpperCase()]:Date.CultureInfo.abbreviatedTimeZoneStandard[s.toUpperCase()];};Date.getTimezoneAbbreviation=function(offset,dst){var n=(dst||false)?Date.CultureInfo.abbreviatedTimeZoneDST:Date.CultureInfo.abbreviatedTimeZoneStandard,p;for(p in n){if(n[p]===offset){return p;}}
return null;};Date.prototype.clone=function(){return new Date(this.getTime());};Date.prototype.compareTo=function(date){if(isNaN(this)){throw new Error(this);}
if(date instanceof Date&&!isNaN(date)){return(this>date)?1:(this<date)?-1:0;}else{throw new TypeError(date);}};Date.prototype.equals=function(date){return(this.compareTo(date)===0);};Date.prototype.between=function(start,end){var t=this.getTime();return t>=start.getTime()&&t<=end.getTime();};Date.prototype.addMilliseconds=function(value){this.setMilliseconds(this.getMilliseconds()+value);return this;};Date.prototype.addSeconds=function(value){return this.addMilliseconds(value*1000);};Date.prototype.addMinutes=function(value){return this.addMilliseconds(value*60000);};Date.prototype.addHours=function(value){return this.addMilliseconds(value*3600000);};Date.prototype.addDays=function(value){return this.addMilliseconds(value*86400000);};Date.prototype.addWeeks=function(value){return this.addMilliseconds(value*604800000);};Date.prototype.addMonths=function(value){var n=this.getDate();this.setDate(1);this.setMonth(this.getMonth()+value);this.setDate(Math.min(n,this.getDaysInMonth()));return this;};Date.prototype.addYears=function(value){return this.addMonths(value*12);};Date.prototype.add=function(config){if(typeof config=="number"){this._orient=config;return this;}
var x=config;if(x.millisecond||x.milliseconds){this.addMilliseconds(x.millisecond||x.milliseconds);}
if(x.second||x.seconds){this.addSeconds(x.second||x.seconds);}
if(x.minute||x.minutes){this.addMinutes(x.minute||x.minutes);}
if(x.hour||x.hours){this.addHours(x.hour||x.hours);}
if(x.month||x.months){this.addMonths(x.month||x.months);}
if(x.year||x.years){this.addYears(x.year||x.years);}
if(x.day||x.days){this.addDays(x.day||x.days);}
return this;};Date._validate=function(value,min,max,name){if(typeof value!="number"){throw new TypeError(value+" is not a Number.");}else if(value<min||value>max){throw new RangeError(value+" is not a valid value for "+name+".");}
return true;};Date.validateMillisecond=function(n){return Date._validate(n,0,999,"milliseconds");};Date.validateSecond=function(n){return Date._validate(n,0,59,"seconds");};Date.validateMinute=function(n){return Date._validate(n,0,59,"minutes");};Date.validateHour=function(n){return Date._validate(n,0,23,"hours");};Date.validateDay=function(n,year,month){return Date._validate(n,1,Date.getDaysInMonth(year,month),"days");};Date.validateMonth=function(n){return Date._validate(n,0,11,"months");};Date.validateYear=function(n){return Date._validate(n,1,9999,"seconds");};Date.prototype.set=function(config){var x=config;if(!x.millisecond&&x.millisecond!==0){x.millisecond=-1;}
if(!x.second&&x.second!==0){x.second=-1;}
if(!x.minute&&x.minute!==0){x.minute=-1;}
if(!x.hour&&x.hour!==0){x.hour=-1;}
if(!x.day&&x.day!==0){x.day=-1;}
if(!x.month&&x.month!==0){x.month=-1;}
if(!x.year&&x.year!==0){x.year=-1;}
if(x.millisecond!=-1&&Date.validateMillisecond(x.millisecond)){this.addMilliseconds(x.millisecond-this.getMilliseconds());}
if(x.second!=-1&&Date.validateSecond(x.second)){this.addSeconds(x.second-this.getSeconds());}
if(x.minute!=-1&&Date.validateMinute(x.minute)){this.addMinutes(x.minute-this.getMinutes());}
if(x.hour!=-1&&Date.validateHour(x.hour)){this.addHours(x.hour-this.getHours());}
if(x.month!==-1&&Date.validateMonth(x.month)){this.addMonths(x.month-this.getMonth());}
if(x.year!=-1&&Date.validateYear(x.year)){this.addYears(x.year-this.getFullYear());}
if(x.day!=-1&&Date.validateDay(x.day,this.getFullYear(),this.getMonth())){this.addDays(x.day-this.getDate());}
if(x.timezone){this.setTimezone(x.timezone);}
if(x.timezoneOffset){this.setTimezoneOffset(x.timezoneOffset);}
return this;};Date.prototype.clearTime=function(){this.setHours(0);this.setMinutes(0);this.setSeconds(0);this.setMilliseconds(0);return this;};Date.prototype.isLeapYear=function(){var y=this.getFullYear();return(((y%4===0)&&(y%100!==0))||(y%400===0));};Date.prototype.isWeekday=function(){return!(this.is().sat()||this.is().sun());};Date.prototype.getDaysInMonth=function(){return Date.getDaysInMonth(this.getFullYear(),this.getMonth());};Date.prototype.moveToFirstDayOfMonth=function(){return this.set({day:1});};Date.prototype.moveToLastDayOfMonth=function(){return this.set({day:this.getDaysInMonth()});};Date.prototype.moveToDayOfWeek=function(day,orient){var diff=(day-this.getDay()+7*(orient||+1))%7;return this.addDays((diff===0)?diff+=7*(orient||+1):diff);};Date.prototype.moveToMonth=function(month,orient){var diff=(month-this.getMonth()+12*(orient||+1))%12;return this.addMonths((diff===0)?diff+=12*(orient||+1):diff);};Date.prototype.getDayOfYear=function(){return Math.floor((this-new Date(this.getFullYear(),0,1))/86400000);};Date.prototype.getWeekOfYear=function(firstDayOfWeek){var y=this.getFullYear(),m=this.getMonth(),d=this.getDate();var dow=firstDayOfWeek||Date.CultureInfo.firstDayOfWeek;var offset=7+1-new Date(y,0,1).getDay();if(offset==8){offset=1;}
var daynum=((Date.UTC(y,m,d,0,0,0)-Date.UTC(y,0,1,0,0,0))/86400000)+1;var w=Math.floor((daynum-offset+7)/7);if(w===dow){y--;var prevOffset=7+1-new Date(y,0,1).getDay();if(prevOffset==2||prevOffset==8){w=53;}else{w=52;}}
return w;};Date.prototype.isDST=function(){console.log('isDST');return this.toString().match(/(E|C|M|P)(S|D)T/)[2]=="D";};Date.prototype.getTimezone=function(){return Date.getTimezoneAbbreviation(this.getUTCOffset,this.isDST());};Date.prototype.setTimezoneOffset=function(s){var here=this.getTimezoneOffset(),there=Number(s)*-6/10;this.addMinutes(there-here);return this;};Date.prototype.setTimezone=function(s){return this.setTimezoneOffset(Date.getTimezoneOffset(s));};Date.prototype.getUTCOffset=function(){var n=this.getTimezoneOffset()*-10/6,r;if(n<0){r=(n-10000).toString();return r[0]+r.substr(2);}else{r=(n+10000).toString();return"+"+r.substr(1);}};Date.prototype.getDayName=function(abbrev){return abbrev?Date.CultureInfo.abbreviatedDayNames[this.getDay()]:Date.CultureInfo.dayNames[this.getDay()];};Date.prototype.getMonthName=function(abbrev){return abbrev?Date.CultureInfo.abbreviatedMonthNames[this.getMonth()]:Date.CultureInfo.monthNames[this.getMonth()];};Date.prototype._toString=Date.prototype.toString;Date.prototype.toString=function(format){var self=this;var p=function p(s){return(s.toString().length==1)?"0"+s:s;};return format?format.replace(/dd?d?d?|MM?M?M?|yy?y?y?|hh?|HH?|mm?|ss?|tt?|zz?z?/g,function(format){switch(format){case"hh":return p(self.getHours()<13?self.getHours():(self.getHours()-12));case"h":return self.getHours()<13?self.getHours():(self.getHours()-12);case"HH":return p(self.getHours());case"H":return self.getHours();case"mm":return p(self.getMinutes());case"m":return self.getMinutes();case"ss":return p(self.getSeconds());case"s":return self.getSeconds();case"yyyy":return self.getFullYear();case"yy":return self.getFullYear().toString().substring(2,4);case"dddd":return self.getDayName();case"ddd":return self.getDayName(true);case"dd":return p(self.getDate());case"d":return self.getDate().toString();case"MMMM":return self.getMonthName();case"MMM":return self.getMonthName(true);case"MM":return p((self.getMonth()+1));case"M":return self.getMonth()+1;case"t":return self.getHours()<12?Date.CultureInfo.amDesignator.substring(0,1):Date.CultureInfo.pmDesignator.substring(0,1);case"tt":return self.getHours()<12?Date.CultureInfo.amDesignator:Date.CultureInfo.pmDesignator;case"zzz":case"zz":case"z":return"";}}):this._toString();};
Date.now=function(){return new Date();};Date.today=function(){return Date.now().clearTime();};Date.prototype._orient=+1;Date.prototype.next=function(){this._orient=+1;return this;};Date.prototype.last=Date.prototype.prev=Date.prototype.previous=function(){this._orient=-1;return this;};Date.prototype._is=false;Date.prototype.is=function(){this._is=true;return this;};Number.prototype._dateElement="day";Number.prototype.fromNow=function(){var c={};c[this._dateElement]=this;return Date.now().add(c);};Number.prototype.ago=function(){var c={};c[this._dateElement]=this*-1;return Date.now().add(c);};(function(){var $D=Date.prototype,$N=Number.prototype;var dx=("sunday monday tuesday wednesday thursday friday saturday").split(/\s/),mx=("january february march april may june july august september october november december").split(/\s/),px=("Millisecond Second Minute Hour Day Week Month Year").split(/\s/),de;var df=function(n){return function(){if(this._is){this._is=false;return this.getDay()==n;}
return this.moveToDayOfWeek(n,this._orient);};};for(var i=0;i<dx.length;i++){$D[dx[i]]=$D[dx[i].substring(0,3)]=df(i);}
var mf=function(n){return function(){if(this._is){this._is=false;return this.getMonth()===n;}
return this.moveToMonth(n,this._orient);};};for(var j=0;j<mx.length;j++){$D[mx[j]]=$D[mx[j].substring(0,3)]=mf(j);}
var ef=function(j){return function(){if(j.substring(j.length-1)!="s"){j+="s";}
return this["add"+j](this._orient);};};var nf=function(n){return function(){this._dateElement=n;return this;};};for(var k=0;k<px.length;k++){de=px[k].toLowerCase();$D[de]=$D[de+"s"]=ef(px[k]);$N[de]=$N[de+"s"]=nf(de);}}());Date.prototype.toJSONString=function(){return this.toString("yyyy-MM-ddThh:mm:ssZ");};Date.prototype.toShortDateString=function(){return this.toString(Date.CultureInfo.formatPatterns.shortDatePattern);};Date.prototype.toLongDateString=function(){return this.toString(Date.CultureInfo.formatPatterns.longDatePattern);};Date.prototype.toShortTimeString=function(){return this.toString(Date.CultureInfo.formatPatterns.shortTimePattern);};Date.prototype.toLongTimeString=function(){return this.toString(Date.CultureInfo.formatPatterns.longTimePattern);};Date.prototype.getOrdinal=function(){switch(this.getDate()){case 1:case 21:case 31:return"st";case 2:case 22:return"nd";case 3:case 23:return"rd";default:return"th";}};
(function(){Date.Parsing={Exception:function(s){this.message="Parse error at '"+s.substring(0,10)+" ...'";}};var $P=Date.Parsing;var _=$P.Operators={rtoken:function(r){return function(s){var mx=s.match(r);if(mx){return([mx[0],s.substring(mx[0].length)]);}else{throw new $P.Exception(s);}};},token:function(s){return function(s){return _.rtoken(new RegExp("^\s*"+s+"\s*"))(s);};},stoken:function(s){return _.rtoken(new RegExp("^"+s));},until:function(p){return function(s){var qx=[],rx=null;while(s.length){try{rx=p.call(this,s);}catch(e){qx.push(rx[0]);s=rx[1];continue;}
break;}
return[qx,s];};},many:function(p){return function(s){var rx=[],r=null;while(s.length){try{r=p.call(this,s);}catch(e){return[rx,s];}
rx.push(r[0]);s=r[1];}
return[rx,s];};},optional:function(p){return function(s){var r=null;try{r=p.call(this,s);}catch(e){return[null,s];}
return[r[0],r[1]];};},not:function(p){return function(s){try{p.call(this,s);}catch(e){return[null,s];}
throw new $P.Exception(s);};},ignore:function(p){return p?function(s){var r=null;r=p.call(this,s);return[null,r[1]];}:null;},product:function(){var px=arguments[0],qx=Array.prototype.slice.call(arguments,1),rx=[];for(var i=0;i<px.length;i++){rx.push(_.each(px[i],qx));}
return rx;},cache:function(rule){var cache={},r=null;return function(s){try{r=cache[s]=(cache[s]||rule.call(this,s));}catch(e){r=cache[s]=e;}
if(r instanceof $P.Exception){throw r;}else{return r;}};},any:function(){var px=arguments;return function(s){var r=null;for(var i=0;i<px.length;i++){if(px[i]==null){continue;}
try{r=(px[i].call(this,s));}catch(e){r=null;}
if(r){return r;}}
throw new $P.Exception(s);};},each:function(){var px=arguments;return function(s){var rx=[],r=null;for(var i=0;i<px.length;i++){if(px[i]==null){continue;}
try{r=(px[i].call(this,s));}catch(e){throw new $P.Exception(s);}
rx.push(r[0]);s=r[1];}
return[rx,s];};},all:function(){var px=arguments,_=_;return _.each(_.optional(px));},sequence:function(px,d,c){d=d||_.rtoken(/^\s*/);c=c||null;if(px.length==1){return px[0];}
return function(s){var r=null,q=null;var rx=[];for(var i=0;i<px.length;i++){try{r=px[i].call(this,s);}catch(e){break;}
rx.push(r[0]);try{q=d.call(this,r[1]);}catch(ex){q=null;break;}
s=q[1];}
if(!r){throw new $P.Exception(s);}
if(q){throw new $P.Exception(q[1]);}
if(c){try{r=c.call(this,r[1]);}catch(ey){throw new $P.Exception(r[1]);}}
return[rx,(r?r[1]:s)];};},between:function(d1,p,d2){d2=d2||d1;var _fn=_.each(_.ignore(d1),p,_.ignore(d2));return function(s){var rx=_fn.call(this,s);return[[rx[0][0],r[0][2]],rx[1]];};},list:function(p,d,c){d=d||_.rtoken(/^\s*/);c=c||null;return(p instanceof Array?_.each(_.product(p.slice(0,-1),_.ignore(d)),p.slice(-1),_.ignore(c)):_.each(_.many(_.each(p,_.ignore(d))),px,_.ignore(c)));},set:function(px,d,c){d=d||_.rtoken(/^\s*/);c=c||null;return function(s){var r=null,p=null,q=null,rx=null,best=[[],s],last=false;for(var i=0;i<px.length;i++){q=null;p=null;r=null;last=(px.length==1);try{r=px[i].call(this,s);}catch(e){continue;}
rx=[[r[0]],r[1]];if(r[1].length>0&&!last){try{q=d.call(this,r[1]);}catch(ex){last=true;}}else{last=true;}
if(!last&&q[1].length===0){last=true;}
if(!last){var qx=[];for(var j=0;j<px.length;j++){if(i!=j){qx.push(px[j]);}}
p=_.set(qx,d).call(this,q[1]);if(p[0].length>0){rx[0]=rx[0].concat(p[0]);rx[1]=p[1];}}
if(rx[1].length<best[1].length){best=rx;}
if(best[1].length===0){break;}}
if(best[0].length===0){return best;}
if(c){try{q=c.call(this,best[1]);}catch(ey){throw new $P.Exception(best[1]);}
best[1]=q[1];}
return best;};},forward:function(gr,fname){return function(s){return gr[fname].call(this,s);};},replace:function(rule,repl){return function(s){var r=rule.call(this,s);return[repl,r[1]];};},process:function(rule,fn){return function(s){var r=rule.call(this,s);return[fn.call(this,r[0]),r[1]];};},min:function(min,rule){return function(s){var rx=rule.call(this,s);if(rx[0].length<min){throw new $P.Exception(s);}
return rx;};}};var _generator=function(op){return function(){var args=null,rx=[];if(arguments.length>1){args=Array.prototype.slice.call(arguments);}else if(arguments[0]instanceof Array){args=arguments[0];}
if(args){for(var i=0,px=args.shift();i<px.length;i++){args.unshift(px[i]);rx.push(op.apply(null,args));args.shift();return rx;}}else{return op.apply(null,arguments);}};};var gx="optional not ignore cache".split(/\s/);for(var i=0;i<gx.length;i++){_[gx[i]]=_generator(_[gx[i]]);}
var _vector=function(op){return function(){if(arguments[0]instanceof Array){return op.apply(null,arguments[0]);}else{return op.apply(null,arguments);}};};var vx="each any all".split(/\s/);for(var j=0;j<vx.length;j++){_[vx[j]]=_vector(_[vx[j]]);}}());(function(){var flattenAndCompact=function(ax){var rx=[];for(var i=0;i<ax.length;i++){if(ax[i]instanceof Array){rx=rx.concat(flattenAndCompact(ax[i]));}else{if(ax[i]){rx.push(ax[i]);}}}
return rx;};Date.Grammar={};Date.Translator={hour:function(s){return function(){this.hour=Number(s);};},minute:function(s){return function(){this.minute=Number(s);};},second:function(s){return function(){this.second=Number(s);};},meridian:function(s){return function(){this.meridian=s.slice(0,1).toLowerCase();};},timezone:function(s){return function(){var n=s.replace(/[^\d\+\-]/g,"");if(n.length){this.timezoneOffset=Number(n);}else{this.timezone=s.toLowerCase();}};},day:function(x){var s=x[0];return function(){this.day=Number(s.match(/\d+/)[0]);};},month:function(s){return function(){this.month=((s.length==3)?Date.getMonthNumberFromName(s):(Number(s)-1));};},year:function(s){return function(){var n=Number(s);this.year=((s.length>2)?n:(n+(((n+2000)<Date.CultureInfo.twoDigitYearMax)?2000:1900)));};},rday:function(s){return function(){switch(s){case"yesterday":this.days=-1;break;case"tomorrow":this.days=1;break;case"today":this.days=0;break;case"now":this.days=0;this.now=true;break;}};},finishExact:function(x){x=(x instanceof Array)?x:[x];var now=new Date();this.year=now.getFullYear();this.month=now.getMonth();this.day=1;this.hour=0;this.minute=0;this.second=0;for(var i=0;i<x.length;i++){if(x[i]){x[i].call(this);}}
this.hour=(this.meridian=="p"&&this.hour<13)?this.hour+12:this.hour;if(this.day>Date.getDaysInMonth(this.year,this.month)){throw new RangeError(this.day+" is not a valid value for days.");}
var r=new Date(this.year,this.month,this.day,this.hour,this.minute,this.second);if(this.timezone){r.set({timezone:this.timezone});}else if(this.timezoneOffset){r.set({timezoneOffset:this.timezoneOffset});}
return r;},finish:function(x){x=(x instanceof Array)?flattenAndCompact(x):[x];if(x.length===0){return null;}
for(var i=0;i<x.length;i++){if(typeof x[i]=="function"){x[i].call(this);}}
if(this.now){return new Date();}
var today=Date.today();var method=null;var expression=!!(this.days!=null||this.orient||this.operator);if(expression){var gap,mod,orient;orient=((this.orient=="past"||this.operator=="subtract")?-1:1);if(this.weekday){this.unit="day";gap=(Date.getDayNumberFromName(this.weekday)-today.getDay());mod=7;this.days=gap?((gap+(orient*mod))%mod):(orient*mod);}
if(this.month){this.unit="month";gap=(this.month-today.getMonth());mod=12;this.months=gap?((gap+(orient*mod))%mod):(orient*mod);this.month=null;}
if(!this.unit){this.unit="day";}
if(this[this.unit+"s"]==null||this.operator!=null){if(!this.value){this.value=1;}
if(this.unit=="week"){this.unit="day";this.value=this.value*7;}
this[this.unit+"s"]=this.value*orient;}
return today.add(this);}else{if(this.meridian&&this.hour){this.hour=(this.hour<13&&this.meridian=="p")?this.hour+12:this.hour;}
if(this.weekday&&!this.day){this.day=(today.addDays((Date.getDayNumberFromName(this.weekday)-today.getDay()))).getDate();}
if(this.month&&!this.day){this.day=1;}
return today.set(this);}}};var _=Date.Parsing.Operators,g=Date.Grammar,t=Date.Translator,_fn;g.datePartDelimiter=_.rtoken(/^([\s\-\.\,\/\x27]+)/);g.timePartDelimiter=_.stoken(":");g.whiteSpace=_.rtoken(/^\s*/);g.generalDelimiter=_.rtoken(/^(([\s\,]|at|on)+)/);var _C={};g.ctoken=function(keys){var fn=_C[keys];if(!fn){var c=Date.CultureInfo.regexPatterns;var kx=keys.split(/\s+/),px=[];for(var i=0;i<kx.length;i++){px.push(_.replace(_.rtoken(c[kx[i]]),kx[i]));}
fn=_C[keys]=_.any.apply(null,px);}
return fn;};g.ctoken2=function(key){return _.rtoken(Date.CultureInfo.regexPatterns[key]);};g.h=_.cache(_.process(_.rtoken(/^(0[0-9]|1[0-2]|[1-9])/),t.hour));g.hh=_.cache(_.process(_.rtoken(/^(0[0-9]|1[0-2])/),t.hour));g.H=_.cache(_.process(_.rtoken(/^([0-1][0-9]|2[0-3]|[0-9])/),t.hour));g.HH=_.cache(_.process(_.rtoken(/^([0-1][0-9]|2[0-3])/),t.hour));g.m=_.cache(_.process(_.rtoken(/^([0-5][0-9]|[0-9])/),t.minute));g.mm=_.cache(_.process(_.rtoken(/^[0-5][0-9]/),t.minute));g.s=_.cache(_.process(_.rtoken(/^([0-5][0-9]|[0-9])/),t.second));g.ss=_.cache(_.process(_.rtoken(/^[0-5][0-9]/),t.second));g.hms=_.cache(_.sequence([g.H,g.mm,g.ss],g.timePartDelimiter));g.t=_.cache(_.process(g.ctoken2("shortMeridian"),t.meridian));g.tt=_.cache(_.process(g.ctoken2("longMeridian"),t.meridian));g.z=_.cache(_.process(_.rtoken(/^(\+|\-)?\s*\d\d\d\d?/),t.timezone));g.zz=_.cache(_.process(_.rtoken(/^(\+|\-)\s*\d\d\d\d/),t.timezone));g.zzz=_.cache(_.process(g.ctoken2("timezone"),t.timezone));g.timeSuffix=_.each(_.ignore(g.whiteSpace),_.set([g.tt,g.zzz]));g.time=_.each(_.optional(_.ignore(_.stoken("T"))),g.hms,g.timeSuffix);g.d=_.cache(_.process(_.each(_.rtoken(/^([0-2]\d|3[0-1]|\d)/),_.optional(g.ctoken2("ordinalSuffix"))),t.day));g.dd=_.cache(_.process(_.each(_.rtoken(/^([0-2]\d|3[0-1])/),_.optional(g.ctoken2("ordinalSuffix"))),t.day));g.ddd=g.dddd=_.cache(_.process(g.ctoken("sun mon tue wed thu fri sat"),function(s){return function(){this.weekday=s;};}));g.M=_.cache(_.process(_.rtoken(/^(1[0-2]|0\d|\d)/),t.month));g.MM=_.cache(_.process(_.rtoken(/^(1[0-2]|0\d)/),t.month));g.MMM=g.MMMM=_.cache(_.process(g.ctoken("jan feb mar apr may jun jul aug sep oct nov dec"),t.month));g.y=_.cache(_.process(_.rtoken(/^(\d\d?)/),t.year));g.yy=_.cache(_.process(_.rtoken(/^(\d\d)/),t.year));g.yyy=_.cache(_.process(_.rtoken(/^(\d\d?\d?\d?)/),t.year));g.yyyy=_.cache(_.process(_.rtoken(/^(\d\d\d\d)/),t.year));_fn=function(){return _.each(_.any.apply(null,arguments),_.not(g.ctoken2("timeContext")));};g.day=_fn(g.d,g.dd);g.month=_fn(g.M,g.MMM);g.year=_fn(g.yyyy,g.yy);g.orientation=_.process(g.ctoken("past future"),function(s){return function(){this.orient=s;};});g.operator=_.process(g.ctoken("add subtract"),function(s){return function(){this.operator=s;};});g.rday=_.process(g.ctoken("yesterday tomorrow today now"),t.rday);g.unit=_.process(g.ctoken("minute hour day week month year"),function(s){return function(){this.unit=s;};});g.value=_.process(_.rtoken(/^\d\d?(st|nd|rd|th)?/),function(s){return function(){this.value=s.replace(/\D/g,"");};});g.expression=_.set([g.rday,g.operator,g.value,g.unit,g.orientation,g.ddd,g.MMM]);_fn=function(){return _.set(arguments,g.datePartDelimiter);};g.mdy=_fn(g.ddd,g.month,g.day,g.year);g.ymd=_fn(g.ddd,g.year,g.month,g.day);g.dmy=_fn(g.ddd,g.day,g.month,g.year);g.date=function(s){return((g[Date.CultureInfo.dateElementOrder]||g.mdy).call(this,s));};g.format=_.process(_.many(_.any(_.process(_.rtoken(/^(dd?d?d?|MM?M?M?|yy?y?y?|hh?|HH?|mm?|ss?|tt?|zz?z?)/),function(fmt){if(g[fmt]){return g[fmt];}else{throw Date.Parsing.Exception(fmt);}}),_.process(_.rtoken(/^[^dMyhHmstz]+/),function(s){return _.ignore(_.stoken(s));}))),function(rules){return _.process(_.each.apply(null,rules),t.finishExact);});var _F={};var _get=function(f){return _F[f]=(_F[f]||g.format(f)[0]);};g.formats=function(fx){if(fx instanceof Array){var rx=[];for(var i=0;i<fx.length;i++){rx.push(_get(fx[i]));}
return _.any.apply(null,rx);}else{return _get(fx);}};g._formats=g.formats(["yyyy-MM-ddTHH:mm:ss","ddd, MMM dd, yyyy H:mm:ss tt","ddd MMM d yyyy HH:mm:ss zzz","d"]);g._start=_.process(_.set([g.date,g.time,g.expression],g.generalDelimiter,g.whiteSpace),t.finish);g.start=function(s){try{var r=g._formats.call({},s);if(r[1].length===0){return r;}}catch(e){}
return g._start.call({},s);};}());Date._parse=Date.parse;Date.parse=function(s){var r=null;if(!s){return null;}
try{r=Date.Grammar.start.call({},s);}catch(e){return null;}
return((r[1].length===0)?r[0]:null);};Date.getParseFunction=function(fx){var fn=Date.Grammar.formats(fx);return function(s){var r=null;try{r=fn.call({},s);}catch(e){return null;}
return((r[1].length===0)?r[0]:null);};};Date.parseExact=function(s,fx){return Date.getParseFunction(fx)(s);};
(function ($) {

	//var $c = console;
	var
		_native = false,
		is_canvasTextSupported = null,
		measureContext, // canvas context or table cell
		measureText, // function that measures text width
		info_identifier = "shorten-info",
		options_identifier = "shorten-options";

	$.fn.shorten = function() {

		var userOptions = {},
			args = arguments, // for better minification
			func = args.callee; // dito; and shorter than $.fn.shorten

		if ( args.length ) {

			if ( args[0].constructor == Object ) {
				userOptions = args[0];
			} else if ( args[0] == "options" ) {
				return $(this).eq(0).data(options_identifier);
			} else {
				userOptions = {
					width: parseInt(args[0]),
					tail: args[1]
				};
			}
		}

		this.css("visibility","hidden"); // Hide the element(s) while manipulating them

		// apply options vs. defaults
		var options = $.extend({}, func.defaults, userOptions);


		/**
		 * HERE WE GO!
		 **/
		return this.each(function () {

			var	$this = $(this),
				text = $this.text(),
				numChars = text.length,
				targetWidth,
				tailText = $("<span/>").html(options.tail).text(), // convert html to text
				tailWidth,
				info = {
					shortened: false,
					textOverflow: false
				};

			if ($this.css("float") != "none") {
				targetWidth = options.width || $this.width(); // this let's correctly shorten text in floats, but fucks up the rest
			} else {
				targetWidth = options.width || $this.parent().width();
			}

			if (targetWidth < 0) { // jQuery versions < 1.4.4 return negative values for .width() if display:none is used.
				//$c.log("nonsense target width ", targetWidth);
				return true;
			}

			$this.data(options_identifier, options);

			// for consistency with the text-overflow method (which requires these properties), but not actually neccessary.
			//this.style.display = "block";
			//this.style.overflow = "hidden"; // firefox: a floated li will cause the ul to have a "bottom padding" if this is set.
			this.style.whiteSpace = "nowrap";

			// decide on a method for measuring text width
			if ( is_canvasTextSupported ) {
				//$c.log("canvas");
				measureContext = measureText_initCanvas.call( this );
				measureText = measureText_canvas;

			} else {
				//$c.log("table")
				measureContext = measureText_initTable.call( this );
				measureText = measureText_table;
			}

			var origLength = measureText.call( this, text, measureContext );

			if ( origLength < targetWidth ) {
				//$c.log("nothing to do");
				$this.text( text );
				this.style.visibility = "visible";

				$this.data(info_identifier, info);

				return true;
			}

			if ( options.tooltip ) {
				this.setAttribute("title", text);
			}

			/**
			 * If browser implements text-overflow:ellipsis in CSS and tail is &hellip;/Unicode 8230/(…), use it!
			 * In this case we're doing the measurement above to determine if we need the tooltip.
			 **/
			if ( func._native && !userOptions.width ) {
				//$c.log("css ellipsis");
				var rendered_tail = $("<span>"+options.tail+"</span>").text(); // render tail to find out if it's the ellipsis character.

				if ( rendered_tail.length == 1 && rendered_tail.charCodeAt(0) == 8230 ) {

					$this.text( text );

					// the following three properties are needed for text-overflow to work (tested in Chrome).
					// for consistency now I need to set this everywhere... which probably interferes with users' layout...
					//this.style.whiteSpace = "nowrap";
					this.style.overflow = "hidden";
					//this.style.display = "block";

					this.style[func._native] = "ellipsis";
					this.style.visibility = "visible";

					info.shortened = true;
					info.textOverflow = "ellipsis";
					$this.data(info_identifier, info);

					return true;
				}
			}

			tailWidth = measureText.call( this, tailText, measureContext ); // convert html to text and measure it
			targetWidth = targetWidth - tailWidth;

				//$c.log(text +" + "+ tailText);

			/**
			 * Before we start removing characters one by one, let's try to be more intelligent about this:
			 * If the original string is longer than targetWidth by at least 15% (for safety), then shorten it
			 * to targetWidth + 15% (and re-measure for safety). If the resulting text still is too long (as expected),
			 * use that for further shortening. Else use the original text. This saves a lot of time for text that is
			 * much longer than the desired width.
			 */
			var safeGuess = targetWidth * 1.15; // add 15% to targetWidth for safety before making the cut.

			if ( origLength - safeGuess > 0 ) { // if it's safe to cut, do it.

				var cut_ratio = safeGuess / origLength,
					num_guessText_chars = Math.ceil( numChars * cut_ratio ),
					// looking good: shorten and measure
					guessText = text.substring(0, num_guessText_chars),
					guessTextLength = measureText.call( this, guessText, measureContext );

					//$c.info("safe guess: remove " + (numChars - num_guessText_chars) +" chars");

				if ( guessTextLength > targetWidth ) { // make sure it's not too short!
					text = guessText;
					numChars = text.length;
				}
			}

			// Remove characters one by one until text width <= targetWidth
				//var count = 0;
			do {
				numChars--;
				text = text.substring(0, numChars);
					//count++;
			} while ( measureText.call( this, text, measureContext ) >= targetWidth );

			$this.html( $.trim( $("<span/>").text(text).html() ) + options.tail );
			this.style.visibility = "visible";
				//$c.info(count + " normal truncating cycles...")
				//$c.log("----------------------------------------------------------------------");

			info.shortened = true;
			$this.data(info_identifier, info);

			return true;
		});

		return true;

	};



	var css = document.documentElement.style;

	if ( "textOverflow" in css ) {
		_native = "textOverflow";
	} else if ( "OTextOverflow" in css ) {
		_native = "OTextOverflow";
	}

		// test for canvas support

	if ( typeof Modernizr != 'undefined' && Modernizr.canvastext ) { // if Modernizr has tested for this already use that.
		is_canvasTextSupported = Modernizr.canvastext;
	} else {
		var canvas = document.createElement("canvas");
		is_canvasTextSupported = !!(canvas.getContext && canvas.getContext("2d") && (typeof canvas.getContext("2d").fillText === 'function'));
	}
	
	$.fn.shorten._is_canvasTextSupported = is_canvasTextSupported;
	$.fn.shorten._native = _native;



	function measureText_initCanvas()
	{
		var $this = $(this);
		var canvas = document.createElement("canvas");
			//scanvas.setAttribute("width", 500); canvas.setAttribute("height", 40);
		ctx = canvas.getContext("2d");
		$this.html( canvas );

		/* the rounding is experimental. it fixes a problem with a font size specified as 0.7em which resulted in a computed size of 11.2px.
		  without rounding the measured font was too small. even with rounding the result differs slightly from the table method's results. */
		// Get the current text style. This string uses the same syntax as the CSS font specifier. The order matters!
		ctx.font = $this.css("font-style") +" "+ $this.css("font-variant") +" "+ $this.css("font-weight") +" "+ Math.ceil(parseFloat($this.css("font-size"))) +"px "+ $this.css("font-family");

		return ctx;
	}

	// measurement using canvas
	function measureText_canvas( text, ctx )
	{
			//ctx.fillStyle = "red"; ctx.fillRect (0, 0, 500, 40);
			//ctx.fillStyle = "black"; ctx.fillText(text, 0, 12);

		return ctx.measureText(text).width; // crucial, fast but called too often
	};

	function measureText_initTable()
	{
		var css = "padding:0; margin:0; border:none; font:inherit;";
		var $table = $('<table style="'+ css +'width:auto;zoom:1;position:absolute;"><tr style="'+ css +'"><td style="'+ css +'white-space:nowrap;"></td></tr></table>');
		$td = $("td", $table);

		$(this).html( $table );

		return $td;
	};

	// measurement using table
	function measureText_table( text, $td )
	{
		$td.text( text );

		return $td.width(); // crucial but expensive
	};


	$.fn.shorten.defaults = {
		tail: "&hellip;",
		tooltip: true
	};

})(jQuery);//fgnass.github.com/spin.js#v1.2.5
(function(a,b,c){function g(a,c){var d=b.createElement(a||"div"),e;for(e in c)d[e]=c[e];return d}function h(a){for(var b=1,c=arguments.length;b<c;b++)a.appendChild(arguments[b]);return a}function j(a,b,c,d){var g=["opacity",b,~~(a*100),c,d].join("-"),h=.01+c/d*100,j=Math.max(1-(1-a)/b*(100-h),a),k=f.substring(0,f.indexOf("Animation")).toLowerCase(),l=k&&"-"+k+"-"||"";return e[g]||(i.insertRule("@"+l+"keyframes "+g+"{"+"0%{opacity:"+j+"}"+h+"%{opacity:"+a+"}"+(h+.01)+"%{opacity:1}"+(h+b)%100+"%{opacity:"+a+"}"+"100%{opacity:"+j+"}"+"}",0),e[g]=1),g}function k(a,b){var e=a.style,f,g;if(e[b]!==c)return b;b=b.charAt(0).toUpperCase()+b.slice(1);for(g=0;g<d.length;g++){f=d[g]+b;if(e[f]!==c)return f}}function l(a,b){for(var c in b)a.style[k(a,c)||c]=b[c];return a}function m(a){for(var b=1;b<arguments.length;b++){var d=arguments[b];for(var e in d)a[e]===c&&(a[e]=d[e])}return a}function n(a){var b={x:a.offsetLeft,y:a.offsetTop};while(a=a.offsetParent)b.x+=a.offsetLeft,b.y+=a.offsetTop;return b}var d=["webkit","Moz","ms","O"],e={},f,i=function(){var a=g("style");return h(b.getElementsByTagName("head")[0],a),a.sheet||a.styleSheet}(),o={lines:12,length:7,width:5,radius:10,rotate:0,color:"#000",speed:1,trail:100,opacity:.25,fps:20,zIndex:2e9,className:"spinner",top:"auto",left:"auto"},p=function q(a){if(!this.spin)return new q(a);this.opts=m(a||{},q.defaults,o)};p.defaults={},m(p.prototype,{spin:function(a){this.stop();var b=this,c=b.opts,d=b.el=l(g(0,{className:c.className}),{position:"relative",zIndex:c.zIndex}),e=c.radius+c.length+c.width,h,i;a&&(a.insertBefore(d,a.firstChild||null),i=n(a),h=n(d),l(d,{left:(c.left=="auto"?i.x-h.x+(a.offsetWidth>>1):c.left+e)+"px",top:(c.top=="auto"?i.y-h.y+(a.offsetHeight>>1):c.top+e)+"px"})),d.setAttribute("aria-role","progressbar"),b.lines(d,b.opts);if(!f){var j=0,k=c.fps,m=k/c.speed,o=(1-c.opacity)/(m*c.trail/100),p=m/c.lines;!function q(){j++;for(var a=c.lines;a;a--){var e=Math.max(1-(j+a*p)%m*o,c.opacity);b.opacity(d,c.lines-a,e,c)}b.timeout=b.el&&setTimeout(q,~~(1e3/k))}()}return b},stop:function(){var a=this.el;return a&&(clearTimeout(this.timeout),a.parentNode&&a.parentNode.removeChild(a),this.el=c),this},lines:function(a,b){function e(a,d){return l(g(),{position:"absolute",width:b.length+b.width+"px",height:b.width+"px",background:a,boxShadow:d,transformOrigin:"left",transform:"rotate("+~~(360/b.lines*c+b.rotate)+"deg) translate("+b.radius+"px"+",0)",borderRadius:(b.width>>1)+"px"})}var c=0,d;for(;c<b.lines;c++)d=l(g(),{position:"absolute",top:1+~(b.width/2)+"px",transform:b.hwaccel?"translate3d(0,0,0)":"",opacity:b.opacity,animation:f&&j(b.opacity,b.trail,c,b.lines)+" "+1/b.speed+"s linear infinite"}),b.shadow&&h(d,l(e("#000","0 0 4px #000"),{top:"2px"})),h(a,h(d,e(b.color,"0 0 1px rgba(0,0,0,.1)")));return a},opacity:function(a,b,c){b<a.childNodes.length&&(a.childNodes[b].style.opacity=c)}}),!function(){function a(a,b){return g("<"+a+' xmlns="urn:schemas-microsoft.com:vml" class="spin-vml">',b)}var b=l(g("group"),{behavior:"url(#default#VML)"});!k(b,"transform")&&b.adj?(i.addRule(".spin-vml","behavior:url(#default#VML)"),p.prototype.lines=function(b,c){function f(){return l(a("group",{coordsize:e+" "+e,coordorigin:-d+" "+ -d}),{width:e,height:e})}function k(b,e,g){h(i,h(l(f(),{rotation:360/c.lines*b+"deg",left:~~e}),h(l(a("roundrect",{arcsize:1}),{width:d,height:c.width,left:c.radius,top:-c.width>>1,filter:g}),a("fill",{color:c.color,opacity:c.opacity}),a("stroke",{opacity:0}))))}var d=c.length+c.width,e=2*d,g=-(c.width+c.length)*2+"px",i=l(f(),{position:"absolute",top:g,left:g}),j;if(c.shadow)for(j=1;j<=c.lines;j++)k(j,-2,"progid:DXImageTransform.Microsoft.Blur(pixelradius=2,makeshadow=1,shadowopacity=.3)");for(j=1;j<=c.lines;j++)k(j);return h(b,i)},p.prototype.opacity=function(a,b,c,d){var e=a.firstChild;d=d.shadow&&d.lines||0,e&&b+d<e.childNodes.length&&(e=e.childNodes[b+d],e=e&&e.firstChild,e=e&&e.firstChild,e&&(e.opacity=c))}):f=k(b,"animation")}(),a.Spinner=p})(window,document);(function($) {

	var url1 = /(^|&lt;|\s)(www\..+?\..+?)(\s|&gt;|$)/g,
		url2 = /(^|&lt;|\s)(((https?|ftp):\/\/|mailto:).+?)(\s|&gt;|$)/g,
		target = 'target="_blank"';

	function linkifyThis() {
		var childNodes = this.childNodes,
			i = childNodes.length;
		
		while (i--) {
			var n = childNodes[i];
			if (n.nodeType == 3) {
				var html = $.trim(n.nodeValue);
				if (html) {
					html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;')
							.replace(/>/g, '&gt;').replace(
									url1,
									'$1<a href="http://$2" ' + target
											+ '>$2</a>$3').replace(url2,
									'$1<a href="$2" ' + target + '>$2</a>$5');
					$(n).after(html).remove();
				}
			} else if (n.nodeType == 1
					&& !/^(a|button|textarea)$/i.test(n.tagName)) {
				linkifyThis.call(n);
			}
		}
	}

	$.fn.linkify = function() {
		return this.each(linkifyThis);
	};
})(jQuery);
(function($) {
	var vidWidth = 280,
		vidHeight = 240,
		UID = 0,
		playerID = "__YouTube_Player__",
		
		obj = '<object '
				+'width="' + vidWidth + '" ' 
				+ 'height="' + vidHeight + '" '
				+ '>'
					+ '<param name="movie" value="http://www.youtube.com/v/[vid]&hl=en&fs=1"></param>'
					+ '<param name="allowFullScreen" value="true"></param>'
					+ '<param name="allowscriptaccess" value="always"></param>'
					+ '<param name="wmode" value="transparent">'
					+ '<embed '
						+ 'id="' + playerID + '[UID]" '
						+ 'src="http://www.youtube.com/v/[vid]&hl=en&fs=1&version=3&enablejsapi=1" '
						+ 'type="application/x-shockwave-flash" '
						+ 'allowscriptaccess="always" '
						+ 'allowfullscreen="true" '
						+ 'wmode="transparent" '
						+ 'width="' + vidWidth + '" ' + 'height="' + vidHeight
					+ '">'
					+ '</embed>' 
				+ '</object> ';
	

	addYouTubeEmbededToThis = function() {
		var that = $(this);
		var links = that.children("a:contains('youtube.com/watch')");

		links.each(function(i, link) {
			var vid = $(link).attr("href").match(/((\?v=)(\w[\w|-]*))/g); // end up with ?v=oHg5SJYRHA0
			that.append("<br>");
			if (vid.length != 0) {
				var ytid = vid[0].replace(/\?v=/, ''); // end up with oHg5SJYRHA0
				var player = obj.replace(/\[vid\]/g, ytid).replace(/\[UID\]/g, UID);
				UID += 1;
				that.append(player);
			}
		});

	};

	$.fn.addYouTubeEmbeded = function() {
		return this.each(addYouTubeEmbededToThis);
	};
	
	$.fn.stopAllYouTubePlayers = function() {
		var players = this.find("embed[id^="+playerID+"]");
		if(players && players.length > 0) {
			players.each(function(i, p) {
				p.stopVideo();
			});			
		}
	};
})(jQuery);
var MenuItem = function(id,title) {
	var thisObj = this;
	var isLoading = false;
	var selected = false;	
	
	var listItem = $("<li/>");
		
	var aObj = $("<a/>").appendTo(listItem);
	
	var titleBox = $("<span/>").attr({
		"style": "width:1%;"
	}).appendTo(aObj);
	
	var refreshContainer = $("<div/>")
				.addClass("menuItemExtraInfoArea")
				.css("padding-top","5px")
				.appendTo(aObj).hide();
	
	var loadingContainer = $("<div/>")
				.addClass("menuItemExtraInfoArea")
				.css("padding-top","5px")
				.appendTo(aObj).hide();
	
	var notificationsContainer = $("<div/>")
				.addClass("menuItemExtraInfoArea")
				.appendTo(aObj).hide();
	
	var refresh = $("<img/>").attr({
		"src": "refresh.svg",
		"class": "refreshButton"
	})	.appendTo(refreshContainer);
	
	var notification = new Notification(notificationsContainer)
							.setCounter(0);
	
	listItem.click(function() {
		if(selected == false) {
			eWolf.selectApp(id);
		}	
	});

	refresh.click(function() {
		if(isLoading == false) {
			eWolf.trigger("refresh",[id]);
		}	
	});
	
	function updateView() {
		var w = 145;
		if(selected && !isLoading) {
			refreshContainer.show();
			w = w - 20;
		} else {
			refreshContainer.hide();
		}
		
		if(selected || isLoading) {
			notificationsContainer.hide();
		} else {
			notificationsContainer.show();
		}
		
		if(isLoading) {
			loadingContainer.show();
			w = w - 20;
		} else {
			loadingContainer.hide();
		}
		
		titleBox.text(title).shorten({width:w});
	}	
	
	function select() {
		aObj.addClass("currentMenuSelection");
		selected = true;
		updateView();
	}

	function unselect() {
		aObj.removeClass("currentMenuSelection");
		selected = false;
		updateView();
	}
	
	eWolf.bind("select",function(event,eventId) {
		if(id == eventId) {
			select();
		} else {
			unselect();
		}			
	});
	
	eWolf.bind("loading",function(event,eventId) {
		if(id == eventId) {
			isLoading = true;
			updateView();
			loadingContainer.spin(menuItemSpinnerOpts);
		}	
	});
	
	eWolf.bind("loadingEnd",function(event,eventId) {
		if(id == eventId) {
			isLoading = false;
			updateView();
			loadingContainer.data('spinner').stop();
		}
	});
	
	this.appendTo = function(place) {
		listItem.appendTo(place);
		updateView();
		return thisObj;
	};
	
	this.getId = function() {
		return id;
	};
	
	this.renameTitle = function(newTitle) {
		title = newTitle;
		updateView();
		return thisObj;
	};
	
	this.setNotificationCounter = function(number) {
		if(number < 0) {
			number = 0;
		}
		
		if(notification) {
			notification.setCounter(number);
		}
		
		return thisObj;
	};
	
	this.destroy = function() {
		listItem.remove();
		delete thisObj;
	};
	
	return this;
};

var menuItemSpinnerOpts = {
		  lines: 10, // The number of lines to draw
		  length: 4, // The length of each line
		  width: 2, // The line thickness
		  radius: 3, // The radius of the inner circle
		  rotate: 0, // The rotation offset
		  color: '#000', // #rgb or #rrggbb
		  speed: 0.8, // Rounds per second
		  trail: 60, // Afterglow percentage
		  shadow: false, // Whether to render a shadow
		  hwaccel: false, // Whether to use hardware acceleration
		  className: 'spinner', // The CSS class to assign to the spinner
		  zIndex: 2e9, // The z-index (defaults to 2000000000)
		  top: 0, // Top position relative to parent in px
		  left: 0 // Left position relative to parent in px
		};var MenuList = function(id,title) {
	var self = this;
	
	var items = [];
	
	var frame = $("<div/>").addClass("menuList").hide();
	
	$("<div/>").addClass("menuListTitle")
		.append(title)
		.appendTo(frame);

	var list = $("<ul/>").appendTo(frame);	
	var menuItemList = $("<span/>").appendTo(list);
	var xtraItemList = $("<span/>").appendTo(list);
	
	this.addMenuItem = function(id,title) {
		if(items[id] == null) {
			var menuItem = new MenuItem(id,title)
					.appendTo(menuItemList);
			
			items[id] = menuItem;
			
			if(Object.keys(items).length > 0) {
				frame.show();
			}
		} else {
			console.log("[Menu Error] Item with id: "+ id +" already exist");
		}
		
		return self;
	};
	
	this.addExtraItem = function(item) {
		xtraItemList.append(item);
		return self;
	};
	
	this.removeMenuItem = function(removeId) {
		if(items[removeId] != null) {
			items[removeId].destroy();
			delete items[removeId];
		}
		
		if(Object.keys(items).length <= 0) {
			frame.hide();
		}
		
		return self;
	};
	
	this.renameMenuItem = function(id,newTitle) {
		if(items[id] != null) {
			items[id].renameTitle(newTitle);
		}
		
		return self;
	};
	
	this.hideMenu = function () {
		frame.hide();
		
		return self;
	};
	
	this.showMenu = function () {
		if(Object.keys(items).length > 0) {
			frame.show();
		}
		
		return self;
	};
	
	this.appendTo = function(container) {
		frame.appendTo(container);
		return self;
	};
	
	return this;
};var SideMenu = function(menu, mainFrame) {
	var thisObj = this;
	
	var itemSpace = menu.children("#menuItemsSpace");
	
	var toggleButton = menu.children("#toggleButtons");

	var hideBtn = toggleButton.children("#btnHideMenu"),
		showBtn = toggleButton.children("#btnShowMenu"),
		pinBtn = toggleButton.children("#btnPin"),
		unpinBtn = toggleButton.children("#btnUnPin"),
		menuLists = [];
	
	this.showMenu = function (){
		showBtn.hide();
		thisObj.menuIn();
		thisObj.mainFrameShrink();
		hideBtn.show();
	};

	this.hideMenu = function () {
		hideBtn.hide();
		thisObj.menuOut();
		thisObj.mainFrameGrow();
		showBtn.show();
	};

	this.pinMenu = function () {
		thisObj.mainFrameShrink();
		pinBtn.hide();
		unpinBtn.show();
		hideBtn.show();
		menu.unbind("mouseover");
		menu.unbind("mouseout");
	};

	this.unpinMenu = function () {
		thisObj.mainFrameGrow();
		unpinBtn.hide();
		pinBtn.show();
		hideBtn.hide();
		menu.mouseover(thisObj.menuIn);
		menu.mouseout(thisObj.menuOut);
	};

	this.menuOut = function () {
		menu.stop();
		itemSpace.stop();

		menu.animate({
			opacity : 0.25,
			left : '-175px',
		}, 200, function() {
			itemSpace.animate({
				opacity : 0
			}, 200, function() {
				// Animation complete
			});
		});
	};

	this.menuIn = function () {
		menu.stop();
		itemSpace.stop();

		menu.animate({
			opacity : 0.7,
			left : '-35px',
		}, 200, function() {
			// Animation complete
		});
		
		itemSpace.animate({
			opacity : 1,
		}, 400, function() {
			// Animation complete
		});
		
	};

	this.mainFrameGrow = function () {
		mainFrame.stop();

		mainFrame.animate({
			left : '30px',
			right : '0'
		}, 200, function() {
			eWolf.trigger("mainFrameResize",["sideMenu"]);
		});
	};

	this.mainFrameShrink = function () {
		mainFrame.stop();

		mainFrame.animate({
			left : '170px',
			right : '0'
		}, 200, function() {
			eWolf.trigger("mainFrameResize",["sideMenu"]);
		});
	};
	
	$(window).resize(function() {
		eWolf.trigger("mainFrameResize",["window"]);
	});
	
	this.append = function(item) {
		itemSpace.append(item);
	};
	
	this.createNewMenuList = function(id, title) {
		var menuLst = new MenuList(id,title)
			.appendTo(itemSpace);
		menuLists.push(menuLst);
		return menuLst;
	};
	
	hideBtn.click(this.hideMenu);
	showBtn.click(this.showMenu);
	pinBtn.click(this.pinMenu);
	unpinBtn.click(this.unpinMenu);
	
	return this;
};function CreateMailItemBox(mailObj) {
	var text = mailObj.text;
	var canvas = $("<div/>").html(text);
	canvas.linkify();
	canvas.addYouTubeEmbeded();

	if(mailObj.attachment != null) {
		var imageCanvas = $("<div/>");
		var attachList = $("<div/>").css({
			"margin-left" : "5px"
		});
		
		$.each(mailObj.attachment, function(i, attach) {
			if(attach.contentType.substring(0,5) == "image") {
				var aObj = $("<a/>").attr({
					href: attach.path,
					target: "_TRG_"+attach.filename
				}).appendTo(imageCanvas);
				
				new ThumbnailImage(attach.path,attach.filename,
						0.7,200,100,function(img) {
					img.css({
						"padding" : "5px 5px 5px 5px"
					}).appendTo(aObj);
					
					$("<em/>").append("&nbsp;").appendTo(imageCanvas);
				});
				
			} else {
				var li = $("<li/>").appendTo(attachList);
				
				$("<a/>").attr({
					href: attach.path,
					target: "_TRG_"+attach.filename
				}).append(CreateFileItemBox(attach.filename,
						attach.contentType,attach.size))
					.appendTo(li);
			}
		});
		
		if(! imageCanvas.is(":empty")) {
			imageCanvas.appendTo(canvas);
		}
		
		if(! attachList.is(":empty")) {
			var line = $("<hr/>").css({
				"color" : "#AAA",
				"background-color" : "#AAA",
				"height" : "1px",
				"border" : "0"
			});
			
			$("<div/>")
				.append(line)
				//.append("Attachments:")
				.append(attachList)
				.appendTo(canvas);
		}
	}	
	
	return canvas;
}
var GenericItem = function(senderID,senderName,timestamp,mail,
		listClass,msgBoxClass,preMessageTitle,allowShrink) {
	var thisObj = this;
	var itemSpan = $("<span/>");
	
	var listItem = $("<li/>").attr({
		"class": listClass
	}).appendTo(itemSpan);
	
	var preMessageBox = $("<span/>").attr({
		"style": "width:1%;",
		"class": "preMessageBox"
	}).append(preMessageTitle).appendTo(listItem);	
	
	var isOnSender = false;
	var senderBox = CreateUserBox(senderID,senderName)
		.appendTo(listItem)
		.hover(function() {
			isOnSender = true;
		}, function () {
			isOnSender = false;
		});
	
	
	var timestampBox = CreateTimestampBox(timestamp).appendTo(listItem);
		
	var itsMessage = $("<li/>").attr({
		 "class": msgBoxClass
	 })	.append(CreateMailItemBox(JSON.parse(mail)))
	 	.insertAfter(listItem);
	
	if(allowShrink) {
		itsMessage.hide();
		
		listItem.click(function() {		
			if(!isOnSender){
				itsMessage.toggle();
			}				
		});
	}	
	
	function updateView() {
		var w = listItem.width()-timestampBox.width()-preMessageBox.width()-20;		
		senderBox.text(senderName).shorten({width:w});
	}	
	
	eWolf.bind("mainFrameResize,refresh",function(event,eventId) {
		updateView();
	});
	
	this.appendTo = function(place) {
		itemSpan.appendTo(place);
		updateView();
		return thisObj;
	};
	
	this.prependTo = function(place) {
		itemSpan.prependTo(place);
		updateView();
		return thisObj;
	};
	
	this.insertAfter = function(place) {
		itemSpan.insertAfter(place);
		updateView();
		return thisObj;
	};
	
	this.getListItem = function() {
		return itemSpan;
	};
	
	this.destroy = function() {
		message.destroy();
		itemSpan.remove();
		delete thisObj;
	};
	
	return this;
};var GenericMailList = function(mailType,appID,
		extraDataToSend, maxOlderMessagesFetch,
		listClass,msgBoxClass,preMessageTitle,allowShrink) {
	var self = this;
	
	var newsFeedRequestName = appID + "__newsfeed_request_name__";
	
	var newestDate = null;
	var oldestDate = null;
	
	var lastItem = null;
	
	this.frame = $("<span/>");
	
	var list = $("<ul/>").attr({
		"class" : "messageList"
	}).appendTo(this.frame);
	
	this.updateFromServer = function (getOlder) {
		var data = {};
		$.extend(data,extraDataToSend);
		
		if(getOlder && newestDate != null && oldestDate != null) {
			data.olderThan = oldestDate-1;
		} else if(newestDate != null) {
			data.newerThan = newestDate+1;
		}
		
		if(!data.newerThan) {
			data.maxMessages = maxOlderMessagesFetch;			
		}
		
		var postData = {};
		postData[mailType] = data;
		
		return postData;
	};
	
	this.addItem = function (senderID,senderName,timestamp,mail) {
		 var item = new GenericItem(senderID,senderName,timestamp,mail,
				 listClass,msgBoxClass,preMessageTitle,allowShrink);
		 
		var appended = false;
		 
		if(oldestDate == null || timestamp - oldestDate < 0) {
			 oldestDate = timestamp;
			 item.appendTo(list);
			 appended = true;
		}
		
		if(newestDate == null || timestamp - newestDate > 0) {
			newestDate = timestamp;
			if(!appended) {
				item.prependTo(list);
				appended = true;
			}
		}
		
		if(!appended) {
			item.insertAfter(lastItem.getListItem());
		}
		
		lastItem = item;
		
		return item;
	};
	
	var responseHandler = new ResponseHandler(mailType,
			["mailList"],handleNewData);
	
	eWolf.serverRequest.registerRequest(newsFeedRequestName,this.updateFromServer);
	eWolf.serverRequest.registerHandler(newsFeedRequestName,responseHandler.getHandler());
	eWolf.serverRequest.bindRequest(newsFeedRequestName,appID);
	
	var showMore = new ShowMore(function() {
		eWolf.serverRequest.request(appID,self.updateFromServer (true),
				responseHandler.getHandler());
	}).appendTo(this.frame);
	
	function handleNewData(data, textStatus, postData) {
		$.each(data.mailList, function(j, mailItem) {
			self.addItem(mailItem.senderID,mailItem.senderName,
					mailItem.timestamp, mailItem.mail);
		});
		
		if ( (!postData.newerThan) &&
				data.mailList.length < postData.maxMessages) {
			showMore.hide();
		} else if( (!postData.newerThan) && (!postData.olderThan)
				&& data.mailList.length >= postData.maxMessages) {
			showMore.show();
		}
	}
	
	this.appendTo = function (canvas) {
		self.frame.appendTo(canvas);
		return self;
	};
	
	this.destroy = function() {
		self.frame.remove();
		delete self;
	};
	
	return this;
};

var NewsFeedList = function (appID,serverSettings) {
	var pow = "<img src='wolf-paw.svg' height='18px' style='padding-right:5px;'></img>";
	GenericMailList.call(this,"newsFeed",appID,serverSettings,
			eWolf.NEWSFEED_MAX_OLDER_MESSAGES_FETCH,
			"postListItem","postBox",pow,false);
	
	return this;
};

var WolfpackNewsFeedList = function (appID,wolfpack) {
	var newsFeedRequestObj = {
		newsOf:"wolfpack"
	};
	
	if(wolfpack != null) {
		newsFeedRequestObj.wolfpackName = wolfpack;
	}
	
	NewsFeedList.call(this,appID,newsFeedRequestObj);
	
	return this;
};

var ProfileNewsFeedList = function (appID,profileID) {
	var newsFeedRequestObj = {
		newsOf:"user"
	};
	
	if(profileID != eWolf.profile.getID()) {
		newsFeedRequestObj.userID = profileID;
	}
	
	NewsFeedList.call(this,appID,newsFeedRequestObj);
	
	return this;
};

var InboxList = function (appID) {	
	
	GenericMailList.call(this,"inbox",appID,{},
			eWolf.INBOX_MAX_OLDER_MESSAGES_FETCH,
			"messageListItem","messageBox", ">> ",true);
	
	return this;
};var ShowMore = function (onClick) {
	var self = this;
	var element = null;
	
	this.draw = function () {
		element = $("<div/>").addClass("showMoreClass")
			.append("Show More...")
			.click(onClick);
		
		return self;
	};
	
	this.remove = function() {
		if(element != null) {
			element.remove();
			element = null;
		}
		
		return self;
	};
	
	this.show = function () {
		if(element) {
			element.show(200);
		}
		
		return self;
	};
	
	this.hide = function () {
		if(element) {
			element.hide(200);
		}
		
		return self;
	};
	
	this.appendTo = function (something) {
		if(element) {
			element.appendTo(something);
		}
		
		return self;
	};
	
	self.draw();
	
	return this;
};var Inbox = function (id,applicationFrame) {
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id, applicationFrame, "Inbox");
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.inbox = new InboxList(id).appendTo(this.frame);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/		
	this.title.addFunction("New Message...", function() {
			new NewMessage(id,applicationFrame).select();
		});
	
	return this;
};
var Login = function(id,applicationFrame) {
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id, applicationFrame, "Welcome to eWolf");	
	
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	$.extend(this,LOGIN_CONSTANTS);
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	var itro = $("<div/>").css({
		"font-size" : "12px"
	}).append("If it is your first time using eWolf, please take the time to signup first.");
	
	this.title.appendAtBottomPart(itro);
	
	this.frame.append("<br>");
	
	var login = new TitleArea("Login").appendTo(this.frame);
	
	var username = $("<input/>").attr({
		"type" : "text",
		"placeholder" : "Username"
	});
	
	var usernameError = $("<span/>").addClass("errorArea");
	
	var password = $("<input/>").attr({
		"type" : "password",
		"placeholder" : "Password"
	});
	
	var passwordError = $("<span/>").addClass("errorArea");
	
	var loginError = $("<span/>").addClass("errorArea");
	
	var base = $("<table/>");
	
	var usernameRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Username:")
		.appendTo(usernameRaw);	
	$("<td/>")
		.append(username)
		.append(usernameError)
		.appendTo(usernameRaw);
	
	var passwordRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Password:")
		.appendTo(passwordRaw);	
	$("<td/>")
		.append(password)
		.append(passwordError)
		.appendTo(passwordRaw);
	
	var loginErrorRow = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.appendTo(loginErrorRow);	
	$("<td/>")
		.append(loginError)
		.appendTo(loginErrorRow);
	
	login.appendAtBottomPart(base);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/
	this.title.addFunction("Signup",function() {
		eWolf.selectApp(eWolf.SIGNUP_APP_ID);
	});	
	
	function handleLogin(data, textStatus, postData) {
		eWolf.getUserInformation();
	}
	
	function errorHandler(data, textStatus, postData) {
		loginError.html(data.errorMessage);
		self.clearAll();
	}
	
	function badRequestHandler(data, textStatus, postData) {
		loginError.html("Server Error. Could not login.");
		self.clearAll();
	}
	
	var formValidator = new FormValidator()
			.registerField(self.LOGIN_USERNAME_ID, username, usernameError)
			.registerField(self.LOGIN_PASSWORD_ID, password, passwordError)
			.attachOnSend(function() {
						var handler = new ResponseHandler("login",[])
							.success(handleLogin)
							.error(errorHandler)
							.badResponseHandler(badRequestHandler);
						
						eWolf.serverRequest.request(id,{
							login : {
								username : username.val(),
								password : password.val()
							}
						}, handler.getHandler());
				})
			.addValidator(self.LOGIN_USERNAME_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must specify a user name.")
			.addValidator(self.LOGIN_PASSWORD_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must specify a password.");	
	
	login.addFunction("Login",formValidator.sendForm);
	
	this.clearAll = function() {
		formValidator.clearAllFields();
		return self;
	};
	
	eWolf.bind("refresh",function(event,eventID) {
		if(id == eventID) {
			self.clearAll();
		}
	});
	
	eWolf.serverRequest.bindAppToAnotherApp(id, eWolf.FIRST_EWOLF_LOGIN_REQUEST_ID);
	
	return this;
};var NewMail = function(callerID,applicationFrame,options,		
		createRequestObj,handleResponseCategory,
		allowAttachment,sendTo,sendToQuery, sendToMultipleInOneMessage) {
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	$.extend(this,NEWMAIL_CONSTANTS);	
	var id = self.NEWMAIL_APP_ID_PREFIX + callerID;	
	
	var settings = $.extend({}, self.NEW_MAIL_DAFAULTS, options);
	
	var files = null;
	
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id ,applicationFrame, settings.TITLE);	
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	var base = $("<table/>")
		.addClass("newMainTable")
		.appendTo(this.frame);
	
	var queryRaw = $("<tr/>").appendTo(base);
	$("<td/>")
		.addClass("newMailAlt")
		.append(settings.TO+":")
		.appendTo(queryRaw);	
	var userIdCell = $("<td/>").appendTo(queryRaw);
	
	sendToQuery.appendTo(userIdCell);
	
	if(sendTo != null) {
		sendToQuery.addTagByQuery(sendTo,true);
	}
	
	var msgRaw = $("<tr/>").appendTo(base);
	$("<td/>")
		.addClass("newMailAlt")
		.append(settings.CONTENT+":")
		.appendTo(msgRaw);
	
	var height = 350;
	if(allowAttachment) {
		height = 200;
	}
	
	var messageText = $("<div/>")
		.addClass("textarea-div")
		.attr({
		"style" : "min-height:"+height+"px;",
		"contentEditable" : "true"
	});
	
	$("<td/>").append(messageText)
		.appendTo(msgRaw);
	
	if(allowAttachment) {
		var attacheRaw = $("<tr/>").appendTo(base);
		$("<td/>")
			.addClass("newMailAlt")
			.append(settings.ATTACHMENT+":")
			.appendTo(attacheRaw);
		
		var uploaderArea = $("<td/>").appendTo(attacheRaw);
		files = new FilesBox(uploaderArea);
	}

	var btnRaw = $("<tr/>").appendTo(base);
	
	$("<td/>").appendTo(btnRaw);
	var btnBox = $("<td/>").appendTo(btnRaw);
	
	var operations = new FunctionsArea().appendTo(btnBox);
	
	var errorRaw = $("<tr/>").appendTo(base);
	
	$("<td/>").appendTo(errorRaw);
	var errorBox = $("<td/>").appendTo(errorRaw);
	
	var errorMessage = $("<span/>").attr({
		"class": "errorArea"
	}).appendTo(errorBox);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/		
	eWolf.bind("select",function(event,eventID) {
		if(eventID == id) {
			window.setTimeout(function () {
				messageText.focus();
			}, 0);
		}
	});
	
	eWolf.bind("select",function(event,eventId) {
		if(eventId != id) {
			self.destroy();
		}
	});
		
	function showDeleteSuccessfulDialog(event) {
		var diag = $("<div/>").attr({
			"id" : "dialog-confirm",
			"title" : "Resend to all destinations?"
		}).addClass("DialogClass");
		
		$("<p/>").appendTo(diag).append(
				"You are reseding the message after its failed to arraive to some of its destinations.<br>" + 
				"The message already arrived to some of its destinations.");
		$("<p/>").appendTo(diag).append(
				"<b>Do you want to resend the message to these destinations?</b>");
		
		diag.dialog({
			resizable: true,
			modal: true,
			width: 550,
			buttons: {
				"Send only to failed": function() {
					$( this ).dialog( "close" );
					self.send(event,true);
				},
				"Resend to all": function() {
					$( this ).dialog( "close" );
					sendToQuery.tagList.unmarkTags();
					self.send(event,true);
				},
				Cancel: function() {
					$( this ).dialog( "close" );
				}
			}
		});
	}
	
	this.updateSend = function() {
		if(sendToQuery.tagList.tagCount({markedError:false,markedOK:false}) > 0) {
			self.title.hideAll();
			operations.hideAll();
		} else if(sendToQuery.tagList.tagCount({markedError:true})) {
			self.title.showAll();
			operations.showAll();
		} else {			
			eWolf.trigger("needRefresh",[callerID]);
			self.cancel();
		}		
	};
	
	this.send = function (event,resend) {
		if(sendToQuery.isMissingField(true, " * Please select a destination(s).")) {
			return false;
		}
		
		if(!resend) {
			if(sendToQuery.tagList.match({markedOK:true}).count() > 0) {
				showDeleteSuccessfulDialog(event);
				return false;
			}
		}
		
		if(sendToQuery.tagList.match({markedOK:false}).count() <= 0) {
			self.updateSend();
			return false;
		}
			
		sendToQuery.tagList.unmarkTags({markedError:true});		
		self.updateSend();		
		errorMessage.html("");
		
		self.sendToAll();
	};
	
	this.sendToAll = function () {		
		var msg = messageText.html();

		var mailObject = {
				text: msg
		};
		
		if(sendToMultipleInOneMessage) {
			var destVector = [];
			
			sendToQuery.tagList.foreachTag({markedOK:false},function(destId) {
				destVector.push(destId);
			});
			
			self.sendTo(destVector, mailObject);			
		} else {
			sendToQuery.tagList.foreachTag({markedOK:false},function(destId) {
				self.uploadFilesThenSendTo(destId, mailObject);
			});
		}			
	};
	
	this.uploadFilesThenSendTo = function (dest, mailObject) {
		if(allowAttachment && files) {
			files.uploadAllFiles(dest, function(success, uploadedFiles) {
				if(success) {
					mailObject.attachment = uploadedFiles;
					self.sendTo(dest,mailObject);
				} else {
					errorMessage.html("Some of the files failed to upload...<br>Message did not sent.");
					self.title.showAll();
					operations.showAll();
				}
			});			
		} else {
			self.sendTo(dest, mailObject);
		}	
	};
	
	this.sendTo = function(destId,dataObj) {
		var data = JSON.stringify(dataObj);
		
		var responseHandler = new ResponseHandler(handleResponseCategory,[],null);
		
		responseHandler.success(function(data, textStatus, postData) {
			if($.isArray(destId)) {
				$.each(destId, function(i, id) {
					sendToQuery.tagList.markTagOK(id);
				});
			} else {
				sendToQuery.tagList.markTagOK(destId);
			}
		}).error(function(response, textStatus, postData) {
			if( ! $.isArray(destId)) {
				self.appendFailErrorMessage(destId, response.toString());
			}
		}).addResponseArray("userIDsResult",
				// Condition
				function(response, textStatus, postData) {
					return $.isArray(destId) && response.isGeneralError();
				},
				// Success
				function(pos, response, textStatus, postData) {
					var itemID = postData.userIDs[pos];
					var item = sendToQuery.tagList.match({id:itemID});
					
					item.markOK();
				},
				// Error
				function(pos, response, textStatus, postData) {
					var itemID = postData.userIDs[pos];
					
					self.appendFailErrorMessage(itemID, 
							response.toString());		
				}).complete(function() {
					self.updateSend();
				});
		
		eWolf.serverRequest.request(id,
				createRequestObj(destId,data),
				responseHandler.getHandler());
	};
	
	this.appendFailErrorMessage = function (id, result) {
		var errorMsg = "Failed to arrive at destination: " +
										id + " with error: " + result;
		errorMessage.append(errorMsg+"<br>");
		
		//sendToQuery.tagList.markTagError(id,errorMsg);
		sendToQuery.tagList.match(id == "everyone" ? {markedOK:false} : {id:id}).markError(errorMsg);
	};
	
	this.cancel = function() {
		eWolf.selectApp(callerID);
	};
	
	self.title
		.addFunction("Send", this.send)
		.addFunction("Cancel",this.cancel);
	operations
		.addFunction("Send", this.send)
		.addFunction("Cancel", this.cancel);

	return this;
};

var NewMessage = function(id,applicationFrame,sendToID) {
	function createNewMessageRequestObj(to,msg) {
		return {
			sendMessage: {
				userIDs: to,
				message: msg
			}
		  };
	}
	
	NewMail.call(this,id,applicationFrame,{
			TITLE : "New Message"
		},createNewMessageRequestObj,"sendMessage",false,
		sendToID,new FriendsQueryTagList(300), true);
	
	return this;
};

var NewPost = function(id,applicationFrame,wolfpack) {	
	function createNewPostRequestObj(to,content) {
		return {
			post: {
				wolfpackName: to,
				post: content
			}
		  };
	}	
	
	NewMail.call(this,id,applicationFrame,{
			TITLE : "New Post",
			TO : "Post to",
			CONTENT: "Post"
		},createNewPostRequestObj,"post",true,
			wolfpack,new WolfpackQueryTagList(300), false);
	
	return this;
};
var Profile = function (id,applicationFrame,userID,userName) {
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id, applicationFrame, "Searching profile...");
	
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	
	var profileRequestName = id + "__ProfileRequest__",
			wolfpacksRequestName = id + "__WolfpakcsRequest__";
	
	var waitingForName = [];
	
	var newsFeed = null;
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	var wolfpacksContainer = new CommaSeperatedList("Wolfpakcs");
	this.title.appendAtBottomPart(wolfpacksContainer.getList());
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/	
	var handleProfileResonse = new ResponseHandler("profile",
			["id","name"],handleProfileData);
	
	var handleWolfpacksResponse = new ResponseHandler(
			userID ? "wolfpacks" : "wolfpacksAll",
			["wolfpacksList"],handleWolfpacksData);
	
	if(userID) {
		handleProfileResonse.error(onProfileNotFound)
												.badResponseHandler(onProfileNotFound);
		
		eWolf.serverRequest.registerRequest(profileRequestName,getProfileData);
		eWolf.serverRequest.registerRequest(wolfpacksRequestName,geWolfpacksData);
	} else {
		profileRequestName = eWolf.PROFILE_REQUEST_NAME;
		wolfpacksRequestName = eWolf.WOLFPACKS_REQUEST_NAME;
	}
	
	eWolf.serverRequest.registerHandler(profileRequestName,handleProfileResonse.getHandler());
	eWolf.serverRequest.registerHandler(wolfpacksRequestName,handleWolfpacksResponse.getHandler());
	
	eWolf.serverRequest.bindRequest(profileRequestName,id);
	eWolf.serverRequest.bindRequest(wolfpacksRequestName,id);
	
	if(userID) {
		this.title.addFunction("Send message...", function (event) {
			new NewMessage(id,applicationFrame,userID).select();
		}, true);
		
		this.title.addFunction("Add to wolfpack...", function () {
			new AddToWolfpack(id, userID,self.frame, this, 
					wolfpacksContainer.getItemNames(), 13, 1);
		}, true);
	} else {
		this.title.addFunction("Post", function() {
			new NewPost(id,applicationFrame).select();
		}, true);
	}
	
	function onProfileFound() {		
		self.title.setTitle(CreateUserBox(userID,userName,true));
		eWolf.members.addKnownUsers(userID,userName);
		
		self.title.showAll();
		
		if(newsFeed == null) {			
			newsFeed = new ProfileNewsFeedList(id,userID)
				.appendTo(self.frame);
		} 	
		
		while(waitingForName.length > 0) {
			waitingForName.pop()(userName);
		}
	}
	
	function onProfileNotFound() {
		self.title.setTitle("Profile not found");
		
		self.title.hideAll();
		
		if(newsFeed != null) {			
			newsFeed.destroy();
			newsFeed = null;
		} 
	}
	
	function handleProfileData(data, textStatus, postData) {
		userID = data.id;
		userName = data.name;
		onProfileFound();
	}
	
	function handleWolfpacksData(data, textStatus, postData) {		
		wolfpacksContainer.removeAll();		 

		 $.each(data.wolfpacksList,function(i,pack) {
			 wolfpacksContainer.addItem(CreateWolfpackBox(pack),pack);
		 });
	  }
	
	function getProfileData() {		
		return {	profile: { userID: userID	} };
	}
	
	function geWolfpacksData() {
		return { wolfpacks: {	userID: userID } };
	}
	
	this.onReceiveName = function(nameHandler) {
		if(userName != null) {
			nameHandler(userName);
		} else {
			waitingForName.push(nameHandler);
		}
		
		return self;
	};
	
	this.getID = function() {
		return userID;
	};
	
	this.getName = function() {
		return userName;
	};
	
	return this;
};
var Signup = function(id) {
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id, applicationFrame, "Sign Up");
	
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	$.extend(this,SIGNUP_CONSTANTS);
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	var fullName = $("<input/>").attr({
		"type" : "text",
		"placeholder" : "Full Name"
	});
	
	var fullNameError = $("<span/>").addClass("errorArea");
	
	var username = $("<input/>").attr({
		"type" : "text",
		"placeholder" : "Username"
	});
	
	var usernameError = $("<span/>").addClass("errorArea");
	
	var password = $("<input/>").attr({
		"type" : "password",
		"placeholder" : "Password"
	});
	
	var passwordError = $("<span/>").addClass("errorArea");
	
	var verifyPassword = $("<input/>").attr({
		"type" : "password",
		"placeholder" : "Verify Password"
	});
	
	var verifyPasswordError = $("<span/>").addClass("errorArea");
	
	var signUpError = $("<span/>").addClass("errorArea");
	
	var base = $("<table/>");
	
	var fullNameRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Full Name:")
		.appendTo(fullNameRaw);	
	$("<td/>")
		.append(fullName)
		.append(fullNameError)
		.appendTo(fullNameRaw);
	
	var usernameRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Username:")
		.appendTo(usernameRaw);	
	$("<td/>")
		.append(username)
		.append(usernameError)
		.appendTo(usernameRaw);
	
	var passwordRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Password:")
		.appendTo(passwordRaw);	
	$("<td/>")
		.append(password)
		.append(passwordError)
		.appendTo(passwordRaw);
	
	var verifyPasswordRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.append("Verify Password:")
		.appendTo(verifyPasswordRaw);	
	$("<td/>")
		.append(verifyPassword)
		.append(verifyPasswordError)
		.appendTo(verifyPasswordRaw);
	
	var signUpErrorRow = $("<tr/>").appendTo(base);
	$("<td/>").addClass("loginFieldDescription")
		.appendTo(signUpErrorRow);	
	$("<td/>")
		.append(signUpError)
		.appendTo(signUpErrorRow);
	
	this.title.appendAtBottomPart(base);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/
	function handleSignUp(data, textStatus, postData) {
		eWolf.getUserInformation();
	}
	
	function errorHandler(data, textStatus, postData) {
		signUpError.html(data.errorMessage);
		self.clearAll();
	}
	
	function badRequestHandler(data, textStatus, postData) {
		signUpError.html("Server Error. Could not sign up.");
		self.clearAll();
	}
	
	var formValidator = new FormValidator()
			.registerField(self.SIGNUP_FULL_NAME_ID, fullName, fullNameError)
			.registerField(self.SIGNUP_USERNAME_ID, username, usernameError)
			.registerField(self.SIGNUP_PASSWORD_ID, password, passwordError)
			.registerField(self.SIGNUP_VERIFY_PASSWORD_ID, verifyPassword, verifyPasswordError)
			.attachOnSend(function() {
				var handler = new ResponseHandler("createAccount",[])
					.success(handleSignUp)
					.error(errorHandler)
					.badResponseHandler(badRequestHandler);
				
				eWolf.serverRequest.request(id,{
						createAccount : {
							name : fullName.val(),
							username : username.val(),
							password : password.val()
						}
					}, handler.getHandler());
				})
			.addValidator(self.SIGNUP_FULL_NAME_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must specify a name.")
			.addValidator(self.SIGNUP_USERNAME_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must specify a user name.")
			.addValidator(self.SIGNUP_PASSWORD_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must specify a password.")
			.addValidator(self.SIGNUP_VERIFY_PASSWORD_ID, VALIDATOR_IS_NOT_EMPTY,
					"* Must verify the password.")
			.addValidator(self.SIGNUP_VERIFY_PASSWORD_ID, function(field) {
				return password.val() == field.val();
			},"* Password do not mach.");
			
	this.title.addFunction("Sign Up",formValidator.sendForm);
	
	this.clearAll = function() {
		formValidator.clearAllFields();
		return self;
	};
	
	eWolf.bind("refresh",function(event,eventID) {
		if(id == eventID) {
			self.clearAll();
		}
	});
	
	eWolf.serverRequest.bindAppToAnotherApp(id, eWolf.FIRST_EWOLF_LOGIN_REQUEST_ID);
	
	return this;
};var WolfpackPage = function (id,wolfpackName,applicationFrame) {	
	/****************************************************************************
	 * Base class
	  ***************************************************************************/	
	Application.call(this, id, applicationFrame, 
			wolfpackName == null ? "News Feed" : CreateWolfpackBox(wolfpackName));
			
	/****************************************************************************
	 * Members
	  ***************************************************************************/
	var self = this;
	
	/****************************************************************************
	 * User Interface
	  ***************************************************************************/
	this.feed = new WolfpackNewsFeedList(id,wolfpackName)
			.appendTo(this.frame);
	
	/****************************************************************************
	 * Functionality
	  ***************************************************************************/	
	this.title.addFunction("Post", function() {
		new NewPost(id,applicationFrame,wolfpackName).select();
	});

	if(wolfpackName != null) {
		var addMembers = null;

		var members = new CommaSeperatedList("Members");
		this.title.appendAtBottomPart(members.getList());
		
		this.showAddMembers = function () {
			members.hide(200);
			self.title.hideFunction("Add members...");
			if(addMembers != null) {
				addMembers.remove();
			}
			
			addMembers = $("<span/>");
			self.title.appendAtBottomPart(addMembers);
			
			new AddMembersToWolfpack(id,wolfpackName,members.getItemNames(),
					self.removeAddMemebers).frame.appendTo(addMembers);
		};
		
		this.removeAddMemebers = function () {
			if(addMembers != null) {
				addMembers.hide(200,function() {
					addMembers.remove();
					addMembers = null;
				});			
			}
			
			self.title.showFunction("Add members...");
			members.show(200);
		};
		
		function getWolfpacksMembersData() {
			return {
				wolfpackMembers: {
					wolfpackName: wolfpackName
				}
			};
		}
		
		function handleWolfpacksMembersData(data, textStatus, postData) {
			list = data.membersList;

			members.removeAll();

			$.each(list, function(i, member) {
				members.addItem(CreateUserBox(member.id, member.name),member.id);
			});
		}
		
		var wolfpackMembersRequestName = id + "__wolfpack_members_request_name__";
		var handlerCategory = eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS;
		
		if(wolfpackName == eWolf.APPROVED_WOLFPACK_NAME ||
				wolfpackName == eWolf.APPROVED_ME_WOLFPACK_NAME) {
			wolfpackMembersRequestName = eWolf.APPROVED_MEMBERS_REQUEST_NAME;
		} else {
			eWolf.serverRequest.registerRequest(wolfpackMembersRequestName,
					getWolfpacksMembersData);
		}
		
		if(wolfpackName == eWolf.APPROVED_WOLFPACK_NAME) {
			handlerCategory = eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS1;
		} else if(wolfpackName == eWolf.APPROVED_ME_WOLFPACK_NAME) {
			handlerCategory = eWolf.REQUEST_CATEGORY_WOLFPACK_MEMBERS_ALIAS2;
		}		
		
		eWolf.serverRequest.registerHandler(wolfpackMembersRequestName,
					new ResponseHandler(handlerCategory,
					["membersList"],
					handleWolfpacksMembersData).getHandler())
			.bindRequest(wolfpackMembersRequestName, id);
					
		
		self.title.addFunction("Add members...", this.showAddMembers);
		
		eWolf.bind("select",function(event,eventId) {
			self.removeAddMemebers();
		});
	}
	
	this.getName = function() {
		return wolfpackName;			
	};
	
	return this;
};
