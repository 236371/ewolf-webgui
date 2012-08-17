var eWolfMaster = new function() {
};

var eWolf = $(eWolfMaster);

$(document).ready(function () {
	new Loading($("#loadingFrame"));
	eWolf.applicationFrame = $("#applicationFrame");
	
	eWolf.sideMenu = new SideMenu($("#menu"),$("#mainFrame"),$("#topbarID"));
	eWolf.welcome = eWolf.sideMenu.createNewMenuList("welcome","Welcome");
	eWolf.mainApps = eWolf.sideMenu.createNewMenuList("mainapps","Main");
	eWolf.wolfpacksMenuList = eWolf.sideMenu.createNewMenuList("wolfpacks","Wolfpacks");
	
	getUserInformation();
});

function getUserInformation() {
	var request = new PostRequestHandler("eWolf","/json",0)
		.register(function() {
			return {
				profile: {}
			};
		},new ResponseHandler("profile",
					["id","name"],handleProfileData).getHandler());
	
	eWolf.wolfpacks = new Wolfpacks(eWolf.wolfpacksMenuList,request,eWolf.applicationFrame);
	request.requestAll();
	
	function handleProfileData(data, textStatus, postData) {
		document.title = "eWolf - " + data.name;
			
		eWolf.data('userID',data.id);
		eWolf.data('userName',data.name);
			
		createMainApps();
	}	
}

function createMainApps() {
	eWolf.wolfpacks.addFriend(eWolf.data("userID"), eWolf.data("userName"));
	
	eWolf.mainApps.addMenuItem(eWolf.data("userID"),"My Profile");
	new Profile(eWolf.data("userID"),eWolf.data('userName'),eWolf.applicationFrame);
	
	eWolf.mainApps.addMenuItem("newsFeedApp","News Feed");
	new WolfpackPage("newsFeedApp",null,eWolf.applicationFrame);
	
	eWolf.mainApps.addMenuItem("messages","Messages");
	new Inbox("messages",eWolf.applicationFrame);
	
	new SearchApp("search",eWolf.sideMenu,eWolf.applicationFrame,
			$("#topbarID"));
	
	// Welcome
	eWolf.welcome.addMenuItem("login_welcome_screen","Login");
	new Login("login_welcome_screen",eWolf.applicationFrame);
	
	eWolf.trigger("select",["newsFeedApp"]);
}var Application = function(id,container) {
	var self = this;
	var selected = false;
	var needRefresh = true;
	
	this.frame = $("<div/>").attr({
		"id": id+"ApplicationFrame",
		"class": "applicationContainer"
	})	.appendTo(container)
		.hide();
	
	eWolf.bind("select."+id,function(event,eventId) {
		if(id == eventId) {	
			if(!selected) {
				self.frame.show(0);
				self.frame.animate({
					opacity : 1,
				}, 700, function() {
				});
				
				selected = true;
			}
			
			if(needRefresh) {
				eWolf.trigger("refresh",[id]);
			}
		} else {
			if(selected) {
				self.frame.animate({
					opacity : 0,
				}, 300, function() {
					self.frame.hide(0);
				});
				
				selected = false;
			}				
		}			
	});
	
	eWolf.bind("refresh."+id,function(event,eventId) {	
		if(id == eventId) {
			needRefresh = false;
		}
	});
	
	eWolf.bind("needRefresh."+id,function(event,eventId) {
		needRefresh = true;
		
		if(selected) {
			eWolf.trigger("refresh."+id.replace("+","\\+"),[id]);
		}
	});
	
	
	this.getFrame = function() {
		return self.frame;
	};
	
	this.getId = function() {
		return id;
	};
	
	this.isSelected = function() {
		return selected;
	};
	
	this.destroy = function() {
		eWolf.unbind("select."+id);
		eWolf.unbind("refresh."+id);
		eWolf.unbind("needRefresh."+id);
		self.frame.remove();
		delete self;
	};
	
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
};function CreateFileItemBox(file) {
	var left = $("<div/>").css({
		"text-align" : "left",
		"display": "inline-block"
	}).append(file.name);
	
	var fileSize = 0;
    if (file.size > 1024 * 1024) {
    	fileSize = (Math.round(file.size * 100 / (1024 * 1024)) / 100).toString() + 'MB';
    } else {
    	fileSize = (Math.round(file.size * 100 / 1024) / 100).toString() + 'KB';
    } 
	
	var right = $("<div/>").css({
		"text-align" : "right",
		"font-size" : "10px",
		"display": "inline-block",
		"margin-left" : "5px"
	}).append("(" + fileSize + ")");
	
	return $("<div/>").css({
		"display": "inline-block"
	}).append(left).append(right);
}DATE_FORMAT = "dd/MM/yyyy (HH:mm)";

function CreateTimestampBox(timestamp) {	
	return $("<span/>").addClass("timestampBox")
		.append(new Date(timestamp).toString(DATE_FORMAT));
}function CreateUserBox(id,name) {
	var link = $("<a/>").attr({
		"style": "width:1%;",
		"class": "selectableBox",
		"title": id
	}).click(function() {
		if(id != eWolf.data("userID")) {
			eWolf.trigger("search",[id,name]);
		} else {
			eWolf.trigger("select",[id]);
		}
	});
	
	if (id == null && name != null) {
		id = eWolf.wolfpacks.getFriendID(name);
	} else if (id != null && name == null) {
		name = eWolf.wolfpacks.getFriendName(id);
		if(name == null) {
			name = id;
			var request = new PostRequestHandler(id,"/json",0).request({
						profile: {
							userID: id
						}
					  },
					new ResponseHandler("profile",["name"],
							function(data, textStatus, postData) {
						name = data.name;
						link.text(name);
					}).getHandler());
		}
	} else if (id == null && name == null) {
		return null;
	}
	
	link.text(name);

	return link;
}function CreateWolfpackBox(name) {
	return $("<span/>").attr({
		"style": "width:1%;",
		"class": "selectableBox"
	}).text(name).click(function() {
		eWolf.trigger("select",["__pack__"+name]);
	});
}var FilesBox = function(uploaderArea) {
	var thisObj = this;
	
	var fileselect;
	var filedrag;
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

		filelist = new TagList(true).appendTo(uploaderArea);

		fileselect[0].addEventListener("change", FileSelectHandler, false);

		// file drop
		filedrag[0].addEventListener("dragover", FileDragHover, false);
		filedrag[0].addEventListener("dragleave", FileDragHover, false);
		filedrag[0].addEventListener("drop", FileSelectHandler, false);
		filedrag[0].style.display = "block";
		
		errorBox = $("<div/>")
			.addClass("errorArea")
			.appendTo(uploaderArea);
	}

	// file drag hover
	function FileDragHover(e) {
		e.stopPropagation();
		e.preventDefault();
		e.target.className = (e.type == "dragover" ? "fileDragBoxHover" : "fileDragBox");
	}

	function FileSelectHandler(e) {
		// cancel event and hover styling
		FileDragHover(e);

		// fetch FileList object
		var files = e.target.files || e.dataTransfer.files;

		// process all File objects
		var emptyFile = false;
		for ( var i = 0, f; f = files[i]; i++) {
			if(f.size != 0) {
				filelist.addTag(UID, f, CreateFileItemBox(f), true);
				UID += 1;
			} else {
				emptyFile = true;
			}
		}
		
		if(emptyFile) {
			errorBox.html("Can't upload an empty file or a folder.");
		} else {
			errorBox.html("");
		}
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
	
	this.uploadFile = function(wolfpackName,onComplete) {
		if(!filelist || filelist.match({markedOK:false}).isEmpty()) {
			onComplete(true,[]);
			return this;
		}
		
		filelist.match({markedOK:false}).each(function(id,file) {
			var xhr = new XMLHttpRequest();

			filelist.initProgressBar(id);
			
			/* event listners */
			xhr.upload.addEventListener("progress", function(evt) {
				if (evt.lengthComputable) {
					var percentComplete = Math.round(evt.loaded * 100
							/ evt.total);
					filelist.setProgress(id,percentComplete);
				} else {
					// TODO: waiting animation
				}
			}, false);
			
			xhr.addEventListener("load", function (evt) {
				var obj = JSON.parse(xhr.responseText);
				
				if(obj.result != RESPONSE_RESULT.SUCCESS) {
					thisObj.markError(id);
				} else {
					thisObj.markOK(id,{
						filename: file.name,
						contentType: file.type,
						path: obj.path
					});
				}
				
				isComplete();
			}, false);
			
			xhr.addEventListener("error", function (evt) {
				thisObj.markError(id);
				isComplete();
			}, false);
			
			xhr.addEventListener("abort", function (evt) {
				thisObj.markError(id);
				isComplete();
			}, false);
			
			filelist.setOnRemoveTag(id, function(id) {
				xhr.abort();
			});
			
			function isComplete() {
				if(filelist.match({markedOK:false,markedError:false}).isEmpty()) {
					var success = false;
					if(filelist.match({markedError:true}).isEmpty()) {
						success = true;
					}
					
					return onComplete(success,thisObj.getUploadedFiles());
				}				
			}

			var addr = "/sfsupload?" + "wolfpackName=" + wolfpackName
					+ "&fileName=" + file.name 
					+ "&contentType=" + file.type;


			xhr.open("POST", addr);
			xhr.send(file);
		});
		
		return this;
	};
	
	return this;
};var FunctionsArea = function () {
	var self = this;
	
	this.frame = $("<div/>");
	
	var functions = {};
	
	this.appendTo = function (container) {
		self.frame.appendTo(container);
		return self;
	};
	
	this.addFunction = function (functionName,functionOp) {
		if(functions[functionName] == null) {
			functions[functionName] = $("<input/>").attr({
				"type": "button",
				"value": functionName
			}).click(functionOp).appendTo(this.frame);
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
};var PopUp = function(frame, activator) {
	var self = this;
	
	var pos = $(activator).position();

	// .outerWidth() takes into account border and padding.
	var width = $(activator).outerWidth() - 26;
	var height = $(activator).outerHeight();
	
	var leftMargin = parseInt($(activator).css("margin-left"));

	//show the menu directly over the placeholder
	this.frame = $("<div/>").css({
		position : "absolute",
		top : (pos.top + height + 1) + "px",
		left : (pos.left + 13 + leftMargin) + "px",
		width : width,
		"border": "1px solid #999",
		"background-color" : "white"
	}).appendTo(frame).hide();
	
	function clickFunc() {
		if(! self.frame.is(":hover")) {
			self.destroy();
		}		
	};
	
	$(document).bind("click",clickFunc);
	
	this.destroy = function () {
		self.frame.hide(200,function() {
			self.frame.remove();
		});
		 $(document).unbind("click",clickFunc);
		 delete self;
	};
	
	this.frame.show(200);
	
	return this;
};var QueryTagList = function(minWidth,queryPlaceHolder,availableQueries,
		allowMultipleDestinations,commitQuery) {
	var thisObj = this;
	
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
		thisObj.addTagByQuery(query.val(),true);
	}).appendTo(queryBox).hide();
	
	query.autocomplete({
		source: availableQueries,
		select: onSelectSendTo
	}).keyup(function(event) {
	    if(event.keyCode == 13 && query.val() != "") {
	    	thisObj.addTagByQuery(query.val(),true);   	
	    } else {
	    	updateQuery();
	    }	    
	});
	
	function onSelectSendTo(event,ui) {		
		thisObj.addTagByQuery(ui.item.label,true);
		return false;
	}
	
	function updateQuery (id) {	
		if(query.val() == "") {
			addBtn.hide(200);
		} else {
			addBtn.show(200);
		}
		
		if(!allowMultipleDestinations) {
			if(! thisObj.tagList.match().isEmpty()) {
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
		
		if(thisObj.tagList.addTag(res.term,res.term,res.display,removable)) {
			query.val("");
    		updateQuery();
    		return true;
		} else {
			return false;
		}		
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
		var id = eWolf.wolfpacks.getFriendID(query);
		
		if(id == null) {
			id = query;
			query = null;
		}
		
		return {
			term: id,
			display: CreateUserBox(id,query)
		};
	}
	
	return new QueryTagList(minWidth,"Type user name or ID...",
			eWolf.wolfpacks.friendsNameArray,true,sendToFuncReplace);
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
};var BasicRequestHandler = function(id,requestAddress,
		refreshIntervalSec) {
	var self = this;
	
	var observersRequestFunction = [];
	var observersHandleDataFunction = [];
	var timer = null;
	
	function trigger() {
		eWolf.trigger('needRefresh.'+id.replace("+","\\+"),[id]);
	}
	
	function onPostComplete () {
		eWolf.trigger("loadingEnd",[id]);
		
		if(refreshIntervalSec > 0) {
			clearTimeout(timer);
			timer = setTimeout(trigger,refreshIntervalSec*1000);
		}
	}
		
	this.getId = function() {
		return id;
	};
	
	this.setRequestAddress = function(inputRequestAddress) {
		requestAddress = inputRequestAddress;
		return self;
	};
	
	this._makeRequest = function (address,data,success) {
		return self;
	};
	
	this.request = function(data,handleDataFunction) {
		clearTimeout(timer);
		eWolf.trigger("loading",[id]);
		
		self._makeRequest(requestAddress,data,
			function(receivedData,textStatus) {
				handleDataFunction(receivedData,textStatus,data);
			}).complete(onPostComplete);
		
		return self;
	};
	
	this.requestAll = function() {
		var data = {};
		
		$.each(observersRequestFunction, function(i, func) {
			var res = func();
			
			if(res != null) {
				$.extend(data,res);
			}				
		});
		
		this.request(data,function(receivedData,textStatus,data) {
			$.each(observersHandleDataFunction, function(i, func) {
				func(receivedData,textStatus,data);			
			});
		});
		
		return self;
	};
	
	this.register = function(requestFunction,handleDataFunction) {
		if(requestFunction != null && handleDataFunction != null) {
			observersRequestFunction.push(requestFunction);
			observersHandleDataFunction.push(handleDataFunction);
		}
		
		return self;
	};
	
	this.listenToRefresh = function() {
		eWolf.bind("refresh."+id,function(event,eventId) {
			if(id == eventId) {
				self.requestAll();
			}
		});
		
		return self;
	};
		
	return this;
};

var PostRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	BasicRequestHandler.call(this,id,requestAddress,refreshIntervalSec);
	
	this._makeRequest = function (address,data,success) {
		return $.post(address,JSON.stringify(data),success,"json");
	};
	
	return this;
};

var JSONRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	BasicRequestHandler.call(this,id,requestAddress,refreshIntervalSec);
	
	this._makeRequest = function (address,data,success) {		
		return $.getJSON(address,data,success);
	};
	
	return this;
};var RESPONSE_RESULT = {
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

var ResponseHandler = function(category, requiredFields, handler) {
	var thisObj = this;
	
	var errorHandler = null;
	var completeHandler = null;
	var badResponseHandler = null;
	
	function theHandler(data, textStatus, postData) {
		if (data[category] != null) {
			if (data[category].result == RESPONSE_RESULT.SUCCESS) {
				var valid = true;
				$.each(requiredFields, function(i, field) {
					if (field == null) {
						console.log("No field: \"" + field + "\" in response");
						valid = false;
						return false;
					}
				});

				if (valid && handler) {
					handler(data[category], textStatus, postData[category]);
				}
			} else {
				console.log(data[category].result + " : " +
						data[category].errorMessage);
				if(errorHandler) {
					errorHandler(data[category], textStatus, postData[category]);
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
};var Tag = function(id,onRemove,removable,multirow) {
	var box = $("<p/>").attr({
		"class" : "TagClass"
	});
	
	if(!removable) {
		box.addClass("TagNonRemoveable");
	}
	
	if(!multirow) {
		box.addClass("TagNoMultiRow");
	}

	$("<div/>").attr({
		"class" : "TagDeleteClass"
	}).append("&times;").appendTo(box).click(function() {
		box.remove();
		
		if(onRemove) {
			onRemove(id);
		}
	});
	
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
};var TagList = function(multirow,onRemoveTag) {
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
			var newTagItem = new Tag(id,onRemoveTag,removable,multirow)
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
};var TitleArea = function (title) {
	var self = this;
	
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
	
	this.addFunction = function (functionName,functionOp) {
		functions.addFunction(functionName,functionOp);
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
};var Wolfpacks = function (menuList,request,applicationFrame) {
	var self = this;
	
	var wolfpacksApps = {},
		friendsMapByName = {},
		friendsMapByID = {};
	
	this.wolfpacksArray = [];
	this.friendsNameArray = [];
	
	request.register(function() {
		return {
			 wolfpacks:{}
		};
	},new ResponseHandler("wolfpacks",["wolfpacksList"],handleWolfpacks).getHandler());
	
	request.register(function() {
		return {
			wolfpackMembers:{}
		};
	},new ResponseHandler("wolfpackMembers",["membersList"],handleMembers).getHandler());
		
	this.addWolfpack = function (pack) {
		if(wolfpacksApps[pack] == null) {		
			menuList.addMenuItem("__pack__"+pack,pack);			
			var app = new WolfpackPage("__pack__"+pack,pack,applicationFrame);			
			
			wolfpacksApps[pack] = app;
			self.wolfpacksArray.push(pack);
		}
		
		return self;
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
	
	this.addFriend = function(userID,userName) {
		if(friendsMapByName[userName] == null) {
			friendsMapByName[userName] = userID;
			friendsMapByID[userID] = userName;
			self.friendsNameArray.push(userName);
		}		
		
		return self;
	};
	
	this.removeFriend = function(userID,userName) {
		friendsMapByName[userName] = null;
		friendsMapByID[userID] = null;
		
		var idx = self.friendsNameArray.indexOf(userName);
		if(idx != -1){
			self.friendsNameArray.splice(idx, 1);
		}
		
		return self;
	};
	
	this.getFriendID = function (userName) {
		return friendsMapByName[userName];
	};
	
	this.getFriendName = function (userID) {
		return friendsMapByID[userID];
	};
		
	function handleWolfpacks(data, textStatus, postData) {
		$.each(data.wolfpacksList, function(i,pack){
			self.addWolfpack(pack);
		});
	}
	
	function handleMembers(data, textStatus, postData) {
		$.each(data.membersList, function(i,userObj){
			self.addFriend(userObj.id,userObj.name);
		});
	}
	
	return this;
};



/**
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
{ // a dummy block, so I can collapse all the meta stuff in the editor

/*
 * Shorten, a jQuery plugin to automatically shorten text to fit in a block or a pre-set width and configure how the text ends.
 * Copyright (C) 2009-2011  Marc Diethelm
 * License: (GPL 3, http://www.gnu.org/licenses/gpl-3.0.txt) see license.txt
 */


/****************************************************************************
This jQuery plugin automatically shortens text to fit in a block or pre-set width while you can configure how the text ends. The default is an ellipsis  ("…", &hellip;, Unicode: 2026) but you can use anything you want, including markup.

This is achieved using either of two methods: First the the text width of the 'selected' element (eg. span or div) is measured using Canvas or by placing it inside a temporary table cell. If it's too big to big to fit in the element's parent block it is shortened and measured again until it (and the appended ellipsis or text) fits inside the block. A tooltip on the 'selected' element displays the full original text.

If the browser supports truncating text with CSS ('text-overflow:ellipsis') then that is used (but only if the text to append is the default ellipsis). http://www.w3.org/TR/2003/CR-css3-text-20030514/#text-overflow-props

If the text is truncated by the plugin any markup in the text will be stripped (eg: "<a" starts stripping, "< a" does not). This behaviour is dictated by the jQuery .text(val) method. The appended text may contain HTML however (a link or span for example).


Usage Example ('selecting' a div with an id of "element"):

<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
<script type="text/javascript" src="jquery.shorten.js"></script>
<script type="text/javascript">
    $(function() {
        $("#element").shorten();
    });
</script>


By default the plugin will use the parent block's width as the maximum width and an ellipsis as appended text when the text is truncated.

There are three ways of configuring the plugin:

1) Passing a configuration hash as the plugin's argument, eg:

.shorten({
    width: 300,
    tail: ' <a href="#">more</a>',
    tooltip: false
});

2) Using two optional arguments (deprecated!):
width = the desired pixel width, integer
tail = text/html to append when truncating

3) By changing the plugin defaults, eg:


$.fn.shorten.defaults.tail = ' <a href="#">more</a>';


Notes:

There is no default width (unless you create one).

You may want to set the element's CSS to {visibility:hidden;} so it won't
initially flash at full width in slow browsers.

jQuery < 1.4.4: Shorten doesn't work for elements who's parents have display:none, because .width() is broken. (Returns negative values)
http://bugs.jquery.com/ticket/7225
Workarounds:
- Use jQuery 1.4.4+
- Supply a target width in options.
- Use better timing: Don't use display:none when shortening (maybe you can use visibility:hidden). Or shorten after changing display.

Only supports ltr text for now.

Tested with jQuery 1.3+


Based on a creation by M. David Green (www.mdavidgreen.com) in 2009.

Heavily modified/simplified/improved by Marc Diethelm (http://web5.me/).

****************************************************************************/
}


(function ($) {

	//var $c = console;
	var
		_native = false,
		is_canvasTextSupported,
		measureContext, // canvas context or table cell
		measureText, // function that measures text width
		info_identifier = "shorten-info",
		options_identifier = "shorten-options";

	$.fn.shorten = function() {

		var userOptions = {},
			args = arguments, // for better minification
			func = args.callee // dito; and shorter than $.fn.shorten

		if ( args.length ) {

			if ( args[0].constructor == Object ) {
				userOptions = args[0];
			} else if ( args[0] == "options" ) {
				return $(this).eq(0).data(options_identifier);
			} else {
				userOptions = {
					width: parseInt(args[0]),
					tail: args[1]
				}
			}
		}

		this.css("visibility","hidden"); // Hide the element(s) while manipulating them

		// apply options vs. defaults
		var options = $.extend({}, func.defaults, userOptions);


		/**
		 * HERE WE GO!
		 **/
		return this.each(function () {

			var
				$this = $(this),
				text = $this.text(),
				numChars = text.length,
				targetWidth,
				tailText = $("<span/>").html(options.tail).text(), // convert html to text
				tailWidth,
				info = {
					shortened: false,
					textOverflow: false
				}

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
(function(a,b,c){function g(a,c){var d=b.createElement(a||"div"),e;for(e in c)d[e]=c[e];return d}function h(a){for(var b=1,c=arguments.length;b<c;b++)a.appendChild(arguments[b]);return a}function j(a,b,c,d){var g=["opacity",b,~~(a*100),c,d].join("-"),h=.01+c/d*100,j=Math.max(1-(1-a)/b*(100-h),a),k=f.substring(0,f.indexOf("Animation")).toLowerCase(),l=k&&"-"+k+"-"||"";return e[g]||(i.insertRule("@"+l+"keyframes "+g+"{"+"0%{opacity:"+j+"}"+h+"%{opacity:"+a+"}"+(h+.01)+"%{opacity:1}"+(h+b)%100+"%{opacity:"+a+"}"+"100%{opacity:"+j+"}"+"}",0),e[g]=1),g}function k(a,b){var e=a.style,f,g;if(e[b]!==c)return b;b=b.charAt(0).toUpperCase()+b.slice(1);for(g=0;g<d.length;g++){f=d[g]+b;if(e[f]!==c)return f}}function l(a,b){for(var c in b)a.style[k(a,c)||c]=b[c];return a}function m(a){for(var b=1;b<arguments.length;b++){var d=arguments[b];for(var e in d)a[e]===c&&(a[e]=d[e])}return a}function n(a){var b={x:a.offsetLeft,y:a.offsetTop};while(a=a.offsetParent)b.x+=a.offsetLeft,b.y+=a.offsetTop;return b}var d=["webkit","Moz","ms","O"],e={},f,i=function(){var a=g("style");return h(b.getElementsByTagName("head")[0],a),a.sheet||a.styleSheet}(),o={lines:12,length:7,width:5,radius:10,rotate:0,color:"#000",speed:1,trail:100,opacity:.25,fps:20,zIndex:2e9,className:"spinner",top:"auto",left:"auto"},p=function q(a){if(!this.spin)return new q(a);this.opts=m(a||{},q.defaults,o)};p.defaults={},m(p.prototype,{spin:function(a){this.stop();var b=this,c=b.opts,d=b.el=l(g(0,{className:c.className}),{position:"relative",zIndex:c.zIndex}),e=c.radius+c.length+c.width,h,i;a&&(a.insertBefore(d,a.firstChild||null),i=n(a),h=n(d),l(d,{left:(c.left=="auto"?i.x-h.x+(a.offsetWidth>>1):c.left+e)+"px",top:(c.top=="auto"?i.y-h.y+(a.offsetHeight>>1):c.top+e)+"px"})),d.setAttribute("aria-role","progressbar"),b.lines(d,b.opts);if(!f){var j=0,k=c.fps,m=k/c.speed,o=(1-c.opacity)/(m*c.trail/100),p=m/c.lines;!function q(){j++;for(var a=c.lines;a;a--){var e=Math.max(1-(j+a*p)%m*o,c.opacity);b.opacity(d,c.lines-a,e,c)}b.timeout=b.el&&setTimeout(q,~~(1e3/k))}()}return b},stop:function(){var a=this.el;return a&&(clearTimeout(this.timeout),a.parentNode&&a.parentNode.removeChild(a),this.el=c),this},lines:function(a,b){function e(a,d){return l(g(),{position:"absolute",width:b.length+b.width+"px",height:b.width+"px",background:a,boxShadow:d,transformOrigin:"left",transform:"rotate("+~~(360/b.lines*c+b.rotate)+"deg) translate("+b.radius+"px"+",0)",borderRadius:(b.width>>1)+"px"})}var c=0,d;for(;c<b.lines;c++)d=l(g(),{position:"absolute",top:1+~(b.width/2)+"px",transform:b.hwaccel?"translate3d(0,0,0)":"",opacity:b.opacity,animation:f&&j(b.opacity,b.trail,c,b.lines)+" "+1/b.speed+"s linear infinite"}),b.shadow&&h(d,l(e("#000","0 0 4px #000"),{top:"2px"})),h(a,h(d,e(b.color,"0 0 1px rgba(0,0,0,.1)")));return a},opacity:function(a,b,c){b<a.childNodes.length&&(a.childNodes[b].style.opacity=c)}}),!function(){function a(a,b){return g("<"+a+' xmlns="urn:schemas-microsoft.com:vml" class="spin-vml">',b)}var b=l(g("group"),{behavior:"url(#default#VML)"});!k(b,"transform")&&b.adj?(i.addRule(".spin-vml","behavior:url(#default#VML)"),p.prototype.lines=function(b,c){function f(){return l(a("group",{coordsize:e+" "+e,coordorigin:-d+" "+ -d}),{width:e,height:e})}function k(b,e,g){h(i,h(l(f(),{rotation:360/c.lines*b+"deg",left:~~e}),h(l(a("roundrect",{arcsize:1}),{width:d,height:c.width,left:c.radius,top:-c.width>>1,filter:g}),a("fill",{color:c.color,opacity:c.opacity}),a("stroke",{opacity:0}))))}var d=c.length+c.width,e=2*d,g=-(c.width+c.length)*2+"px",i=l(f(),{position:"absolute",top:g,left:g}),j;if(c.shadow)for(j=1;j<=c.lines;j++)k(j,-2,"progid:DXImageTransform.Microsoft.Blur(pixelradius=2,makeshadow=1,shadowopacity=.3)");for(j=1;j<=c.lines;j++)k(j);return h(b,i)},p.prototype.opacity=function(a,b,c,d){var e=a.firstChild;d=d.shadow&&d.lines||0,e&&b+d<e.childNodes.length&&(e=e.childNodes[b+d],e=e&&e.firstChild,e=e&&e.firstChild,e&&(e.opacity=c))}):f=k(b,"animation")}(),a.Spinner=p})(window,document);var MenuItem = function(id,title,messageText,topbarFrame) {
	var thisObj = this;
	var isLoading = false;
	var selected = false;	
	var message = new MenuMessage(messageText,topbarFrame);
	
	var listItem = $("<li/>");
		
	var aObj = $("<a/>").appendTo(listItem);
	
	var titleBox = $("<span/>").attr({
		"style": "width:1%;"
	}).appendTo(aObj);
	
	var refreshContainer = $("<div/>").attr({
		"class": "refreshButtonArea"
	})	.appendTo(aObj).hide();
	
	var loadingContainer = $("<div/>").attr({
		"class": "refreshButtonArea",
		"id": id,
	})	.appendTo(aObj).hide();
	
	var refresh = $("<img/>").attr({
		"src": "refresh.svg",
		"class": "refreshButton"
	})	.appendTo(refreshContainer);
	
	listItem.click(function() {
		if(selected == false) {
			eWolf.trigger("select",[id]);
		}	
	});

	refresh.click(function() {
		if(isLoading == false) {
			eWolf.trigger("refresh."+id.replace("+","\\+"),[id]);
		}	
	});
	
	listItem.mouseover(message.show);
	listItem.mouseout(message.hide);
	
	function updateView() {
		var w = 145;
		if(selected && !isLoading) {
			refreshContainer.show();
			w = w - 20;
		} else {
			refreshContainer.hide();
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
	
	eWolf.bind("select."+id,function(event,eventId) {
		if(id == eventId) {
			select();
		} else {
			unselect();
		}			
	});
	
	eWolf.bind("loading."+id,function(event,eventId) {
		if(id == eventId) {
			isLoading = true;
			updateView();
			loadingContainer.spin(menuItemSpinnerOpts);
		}	
	});
	
	eWolf.bind("loadingEnd."+id,function(event,eventId) {
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
	
	this.destroy = function() {
		message.destroy();
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
		};var MenuList = function(id,title,topbarFrame) {
	var thisObj = this;
	
	var items = [];
	
	var frame = $("<div/>").attr({
		"class" : "menuList"
	}).hide();
	
	$("<div/>").attr({
		"class" : "menuListTitle"
	})	.append(title)
		.appendTo(frame);

	var list = $("<ul/>").appendTo(frame);	
	
	this.addMenuItem = function(id,title) {
		if(items[id] == null) {
			var menuItem = new MenuItem(id,title,
					"Click to show "+title.toLowerCase(),topbarFrame)
					.appendTo(list);
			
			items[id] = menuItem;
			
			if(Object.keys(items).length > 0) {
				frame.show();
			}
		} else {
			console.log("[Menu Error] Item with id: "+ id +" already exist");
		}
		
	};
	
	this.removeMenuItem = function(removeId) {
		if(items[removeId] != null) {
			items[removeId].destroy();
			delete items[removeId];
		}
		
		if(Object.keys(items).length <= 0) {
			frame.hide();
		}
	};
	
	this.renameMenuItem = function(id,newTitle) {
		if(items[id] != null) {
			items[id].renameTitle(newTitle);
		}
	};
	
	this.appendTo = function(container) {
		frame.appendTo(container);
		return thisObj;
	};
	
	return this;
};var MenuMessage = function(text,container) {
	var thisObj = this;	
	var message = null;
	
	this.show = function() {
		if(message == null) {
			message = $("<div/>").attr({
				"class": "menuItemMessageClass"
			}).text(text).appendTo(container);
		} else {
			message.show();
		}
	};
	
	this.hide = function() {
		if(message != null) {
			message.remove();
			message = null;
		}
	};
	
	this.destroy = function() {
		if(message != null) {
			message.remove();
			message = null;
			delete thisObj;
		}
	};
	
	return this;
};

var SideMenu = function(menu, mainFrame,topbarFrame) {
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
		var menuLst = new MenuList(id,title,topbarFrame)
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
	var text = mailObj.text.replace("<","&lt").replace(">","&gt").replace(/\n/g,"<br>");
	var canvas = $("<div/>").html(text);

	if(mailObj.attachment != null) {
		var imageCanvas = $("<div/>");
		var attachCanvas = $("<ul/>");
		
		$.each(mailObj.attachment, function(i, attach) {
			if(attach.contentType.substring(0,5) == "image") {
				var aObj = $("<a/>").attr({
					href: attach.path,
					target: "_TRG_"+attach.filename
				}).appendTo(imageCanvas);
				
				$("<img/>").attr({
					"src": attach.path,
					style: "padding:5px 5px 5px 5px; height:130px;"
				}).appendTo(aObj);				
				
				
				$("<em/>").append("&nbsp;").appendTo(imageCanvas);
			} else {
				var li = $("<li/>").appendTo(attachCanvas);
				
				$("<a/>").attr({
					href: attach.path,
					target: "_TRG_"+attach.filename
				}).append(attach.filename).appendTo(li);
			}
		});
		
		if(! imageCanvas.is(":empty")) {
			imageCanvas.appendTo(canvas);
		}
		
		if(! attachCanvas.is(":empty")) {
			canvas.append("Attachments:");
			attachCanvas.appendTo(canvas);
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
};var GenericMailList = function(mailType,request,serverSettings,
		listClass,msgBoxClass,preMessageTitle,allowShrink) {
	var self = this;
	
	var newestDate = null;
	var oldestDate = null;
	
	var lastItem = null;
	
	this.frame = $("<span/>");
	
	var list = $("<ul/>").attr({
		"class" : "messageList"
	}).appendTo(this.frame);
	
	this.updateFromServer = function (getOlder) {
		var data = {};
		$.extend(data,serverSettings);
		
		if(getOlder && newestDate != null && oldestDate != null) {
			data.olderThan = oldestDate-1;
		} else if(newestDate != null) {
			data.newerThan = newestDate+1;
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
	request.register(this.updateFromServer ,responseHandler.getHandler());
	
	var showMore = new ShowMore(this.frame,function() {
		request.request(self.updateFromServer (true),responseHandler.getHandler());
	}).draw();
	
	function handleNewData(data, textStatus, postData) {
		$.each(data.mailList, function(j, mailItem) {
			self.addItem(mailItem.senderID,mailItem.senderName,
					mailItem.timestamp, mailItem.mail);
		});
		
		if (postData.newerThan == null &&
				data.mailList.length < postData.maxMessages) {
			showMore.remove();
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

var NewsFeedList = function (request,serverSettings) {
	$.extend(serverSettings,{maxMessages:2});
	GenericMailList.call(this,"newsFeed",request,serverSettings,
			"postListItem","postBox","",false);
	
	return this;
};

var WolfpackNewsFeedList = function (request,wolfpack) {
	var newsFeedRequestObj = {
		newsOf:"wolfpack"
	};
	
	if(wolfpack != null) {
		newsFeedRequestObj.wolfpackName = wolfpack;
	}
	
	NewsFeedList.call(this,request,newsFeedRequestObj);
	
	return this;
};

var ProfileNewsFeedList = function (request,profileID) {
	var newsFeedRequestObj = {
		newsOf:"user"
	};
	
	if(profileID != eWolf.data("userID")) {
		newsFeedRequestObj.userID = profileID;
	}
	
	NewsFeedList.call(this,request,newsFeedRequestObj);
	
	return this;
};

var InboxList = function (request,serverSettings) {	
	$.extend(serverSettings,{maxMessages:2});
	GenericMailList.call(this,"inbox",request,serverSettings,
			"messageListItem","messageBox", ">> ",true);
	
	return this;
};var ShowMore = function (frame,onClick) {
	var thisObj = this;
	var element = null;
	
	this.remove = function() {
		if(element != null) {
			element.remove();
		}
		
		return thisObj;
	};
	
	this.draw = function() {
		thisObj.remove();
		
		element = $("<div/>").addClass("showMoreClass")
			.append("Show More...")
			.click(onClick)
			.appendTo(frame);
		
		return thisObj;
	};
};var AddMembersToWolfpack = function(fatherID,wolfpack, existingMemebers,
		onFinish,request) {
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
		
		if(itemsToAdd.isEmpty()) {
			errorMessage.html("Please add new members...");
		} else {
			applyBtn.hide(200);
			cancelBtn.hide(200);
			
			errorMessage.html("");
			
			request.request({
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
			eWolf.trigger("needRefresh."+fatherID.replace("+","\\+"));
		}
		
		delete self;
	};
	
	this.success = function(data, textStatus, postData) {
		madeChanges = true;
		self.cancel();
	};
	
	this.error = function(data, textStatus, postData) {
		var errorMsg = null;
		
		if(data.wolfpacksResult == null) {
			console.log("No wolfpacksResult in response");
		} else if(data.wolfpacksResult[0] != "success") {
			errorMsg = "Error: " + data.wolfpacksResult[0];
			errorMessage.append(errorMsg+"<br>");
		}
		
		if(data.usersResult == null) {
			console.log("No usersResult in response");
		} else {
			$.each(data.usersResult, function(i, result) {
				var itemID = postData.userID[i];
				var item = addMembersQuery.tagList.match({id:itemID});
				
				if(result == "success") {
					madeChanges = true;
					item.unremovable().markOK();
				} else {
					var errorMsg = "Failed to add: " + itemID +
							" with error: " + result;
					errorMessage.append(errorMsg+"<br>");
					
					item.markError(errorMsg);
				}					
			});
		}
		
		if(errorMsg == null) {
			errorMessage.append("Unknown error...<br>");
		}			
	};
	
	this.complete = function (textStatus, postData) {
		if(madeChanges) {
			madeChanges = false;
			eWolf.trigger("needRefresh."+fatherID.replace("+","\\+"));
		}
		
		applyBtn.show(200);
		cancelBtn.show(200);
	};
	
	applyBtn.click(this.apply);	
	cancelBtn.click(this.cancel);
	
	responseHandler
		.success(this.success)	
		.error(this.error)	
		.complete(this.complete);
	
	return this;
};var AddToWolfpack = function(id, frame, activator, request, packsAlreadyIn) {
	var self = this;
	PopUp.call(this,frame,activator);
	
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
	
	this.createWolfpacks = function(wolfpacks,onComplete) {
		if(wolfpacks.length > 0) {			
			var responseHandler = new ResponseHandler("createWolfpack",[],null);
			
			responseHandler.success(function(data, textStatus, postData) {
				$.each(wolfpacks,function(i,pack) {
					eWolf.wolfpacks.addWolfpack(pack);
				});
			}).error(function(data, textStatus, postData) {				
				if(data.wolfpacksResult == null) {
					console.log("No wolfpacksResult in response");
				} else {
					$.each(data.wolfpacksResult, function(i,response) {
						if(response.result == RESPONSE_RESULT.SUCCESS) {
							eWolf.wolfpacks.addWolfpack(postData.wolfpackNames[i]);
						}
					});
					
				}
			}).complete(onComplete);
			
			request.request({
				createWolfpack: {
					wolfpackNames: wolfpacks
				}
			},responseHandler.getHandler());
			
		} else {
			onComplete();
		}
	};
	
	this.addToAllWolfpacks = function (wolfpacks) {
		if(wolfpacks.length > 0) {
			var response = new ResponseHandler("addWolfpackMember",[],null);
			
			response.complete(function (textStatus, postData) {
				eWolf.trigger("needRefresh."+id.replace("+","\\+"));
			});			
			
			request.request({
				addWolfpackMember: {
					wolfpackNames: wolfpacks,
					userIDs: [id]
				}
			},response.getHandler());
		}
	};
	
	this.apply = function() {
		result = self.getSelection();
		
		self.destroy();
		
		self.createWolfpacks(result.create, function () {
			self.addToAllWolfpacks(result.add);
		});
	};
	
	applyBtn.click(this.apply);
		
	return this;
};var Inbox = function (id,applicationFrame) {
	Application.call(this,id,applicationFrame);
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh();
	
	new TitleArea("Inbox")
		.appendTo(this.frame)
		.addFunction("New Message...", function() {
			new NewMessage(id,applicationFrame).select();
		});
	
	new InboxList(request,{}).appendTo(this.frame);
	
	return this;
};
var Login = function(id,applicationFrame) {
	Application.call(this,id,applicationFrame);
	
	var login = new TitleArea("Login").appendTo(this.frame);
	login.addFunction("Login",function() {
		// TODO: Login
		alert("Option unavailible");
	});
	
	var username = $("<input/>").attr({
		"type" : "text",
		"placeholder" : "Username"
	});
	
	var password = $("<input/>").attr({
		"type" : "password",
		"placeholder" : "Password"
	});
	
	var base = $("<table/>");
	
	var usernameRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("newMailAlt")
		.append("Username:")
		.appendTo(usernameRaw);	
	$("<td/>")
		.append(username)
		.appendTo(usernameRaw);
	
	var passwordRaw = $("<tr/>").appendTo(base);
	$("<td/>").addClass("newMailAlt")
		.append("Password:")
		.appendTo(passwordRaw);	
	$("<td/>")
		.append(password)
		.appendTo(passwordRaw);
	
	login.appendAtBottomPart(base);
	
	this.frame.append("<br>");
	new TitleArea("Sign Up").appendTo(this.frame);
	
	return this;
};NEW_MAIL_DESCRIPTION_DAFAULTS = {
	TITLE : "New Mail",
	TO : "To",
	CONTENT : "Content",
	ATTACHMENT : "Attachment"
};

var NewMail = function(callerID,applicationFrame,options,		
		createRequestObj,handleResponseCategory,
		allowAttachment,sendTo,sendToQuery) {
	var self = this;
	var id = "__newmessage__"+callerID;
	
	Application.call(this,id,applicationFrame);
	
	var settings = $.extend({}, NEW_MAIL_DESCRIPTION_DAFAULTS, options);
		
	var request = new PostRequestHandler(id,"/json",0);
		
	var titleArea = new TitleArea(settings.TITLE).appendTo(this.frame);
	
	var base = $("<table/>").appendTo(this.frame);
	
	var queryRaw = $("<tr/>").appendTo(base);
	$("<td/>").attr("class","newMailAlt")
		.append(settings.TO+":")
		.appendTo(queryRaw);	
	var userIdCell = $("<td/>").appendTo(queryRaw);
	
	sendToQuery.appendTo(userIdCell);
	
	if(sendTo != null) {
		sendToQuery.addTagByQuery(sendTo,true);
	}
	
	var msgRaw = $("<tr/>").appendTo(base);
	$("<td/>").attr("class","newMailAlt")
		.append(settings.CONTENT+":")
		.appendTo(msgRaw);
	
	var height = 300;
	if(allowAttachment) {
		height = 100;
	}
	
	var messageText = $("<textarea/>").attr({
		"placeholder": "What is on your mind...",
		"style" : "min-width:300px !important;height:"+height+"px  !important"
	});
	
	$("<td/>").append(messageText).appendTo(msgRaw);
	
	var files = null;
	if(allowAttachment) {
		var attacheRaw = $("<tr/>").appendTo(base);
		$("<td/>").attr("class","newMailAlt")
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
	
	eWolf.bind("refresh",function(event,eventID) {
		if(eventID == id) {
			if(sendTo != null) {				
				window.setTimeout(function () {
					messageText.focus();
				}, 0);				
			} else {
				window.setTimeout(function () {
					sendToQuery.focus();
				}, 0);
			}
		}
	});
	
	eWolf.bind("select."+callerID,function(event,eventId) {
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
			titleArea.hideFunction("Send");
			titleArea.hideFunction("Cancel");
			operations.hideAll();
		} else if(sendToQuery.tagList.tagCount({markedError:true})) {
			titleArea.showFunction("Send");
			titleArea.showFunction("Cancel");
			operations.showAll();
		} else {			
			eWolf.trigger("needRefresh."+callerID.replace("+","\\+"),[callerID]);
			this.cancel();
		}		
	};
	
	this.send = function (event,resend) {
		if(sendToQuery.tagList.isEmpty()) {
			errorMessage.html("Please select a destination(s)");
			return false;
		}

		if(!resend) {
			if(sendToQuery.tagList.match({markedOK:true}).count() > 0) {
				showDeleteSuccessfulDialog(event);
				return false;
			}
		}
			
		sendToQuery.tagList.unmarkTags({markedError:true});		
		self.updateSend();		
		errorMessage.html("");		
		
		self.sendToAll();
	};
	
	this.sendToAll = function () {		
		var msg = messageText.val();
		var mailObject = {
				text: msg
		};
		
		sendToQuery.tagList.foreachTag({markedOK:false},function(destId) {
			if(allowAttachment && files) {
				files.uploadFile(destId, function(success, uploadedFiles) {
					if(success) {
						mailObject.attachment = uploadedFiles;
						self.sendTo(destId,JSON.stringify(mailObject));
					}		
				});			
			} else {
				self.sendTo(destId,JSON.stringify(mailObject));
			}			
		});	
	};
	
	this.sendTo = function(destId,data) {
		var responseHandler = new ResponseHandler(handleResponseCategory,[],null);
		
		responseHandler.success(function(data, textStatus, postData) {
			sendToQuery.tagList.markTagOK(destId);				
			self.updateSend();
		}).error(function(data, textStatus, postData) {
			var errorMsg = "Failed to arrive at destination: " +
			destId + " with error: " + data.result;
			errorMessage.append(errorMsg+"<br>");
			
			sendToQuery.tagList.markTagError(destId,errorMsg);
			self.updateSend();
		});
		
		request.request(
				createRequestObj(destId,data),
				responseHandler.getHandler());
	};
		
	this.select = function() {
		eWolf.trigger("select",[id]);
	};
	
	this.cancel = function() {
		eWolf.trigger("select",[callerID]);
	};
	
	titleArea
		.addFunction("Send", this.send)
		.addFunction("Cancel",this.cancel);
	operations
		.addFunction("Send", this.send)
		.addFunction("Cancel", this.cancel);

	return this;
};

var NewMessage = function(id,applicationFrame,sendToID,sendToName) {	
	function createNewMessageRequestObj(to,msg) {
		return {
			sendMessage: {
				userID: to,
				message: msg
			}
		  };
	}
	
	NewMail.call(this,id,applicationFrame,{
			TITLE : "New Message"
		},createNewMessageRequestObj,"sendMessage",false,
		sendToName,new FriendsQueryTagList(300));
	
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
			wolfpack,new WolfpackQueryTagList(300));
	
	return this;
};
var Profile = function (id,name,applicationFrame) {
	var self = this;
	
	Application.call(this,id,applicationFrame);
	
	var waitingForName = [];
	
	var userObj = {};
	
	if(id != eWolf.data("userID")) {
		userObj.userID = id;
	}
	
	var handleProfileResonse = new ResponseHandler("profile",
			["id","name"],handleProfileData)
		.error(onProfileNotFound);
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh()
		.register(getProfileData,handleProfileResonse.getHandler())
		.register(geWolfpacksData,new ResponseHandler("wolfpacks",
				["wolfpacksList"],handleWolfpacksData).getHandler());
	
	var topTitle = new TitleArea("Searching profile...").appendTo(this.frame);
	
	var idBox = $("<span/>").addClass("idBox");
	topTitle.appendAtTitleTextArea(idBox);
	
	var wolfpacksContainer = new CommaSeperatedList("Wolfpakcs");
	topTitle.appendAtBottomPart(wolfpacksContainer.getList());
	
	if(id != eWolf.data("userID")) {
		topTitle.addFunction("Send message...", function (event) {
			new NewMessage(id,applicationFrame,id,name).select();
		});
		
		topTitle.addFunction("Add to wolfpack...", function () {
			new AddToWolfpack(id, self.frame, this, request, wolfpacksContainer.getItemNames());
			return false;
		});
	} else {
		topTitle.addFunction("Post", function() {
			new NewPost(id,applicationFrame).select();
		});
	}
	
	topTitle.hideAll();
	
	var newsFeed = null;
	
	if(name == null) {
		request.request(getProfileData(),
				handleProfileResonse.getHandler());
	} else {
		onProfileFound();
	}
	
	function onProfileFound() {		
		topTitle.setTitle(CreateUserBox(id,name));
		idBox.html(id);
		
		topTitle.showAll();
		
		if(newsFeed == null) {			
			newsFeed = new ProfileNewsFeedList(request,id)
				.appendTo(self.frame);
		} 	
		
		while(waitingForName.length > 0) {
			waitingForName.pop()(name);
		}
	}
	
	function onProfileNotFound() {
		topTitle.setTitle("Profile not found");
		idBox.html();
		
		topTitle.hideAll();
		
		if(newsFeed != null) {			
			newsFeed.destroy();
			newsFeed = null;
		} 
	}
	
	function handleProfileData(data, textStatus, postData) {		
		name = data.name;
		onProfileFound();
	}
	
	function handleWolfpacksData(data, textStatus, postData) {		
		wolfpacksContainer.removeAll();		 

		 $.each(data.wolfpacksList,function(i,pack) {
			 wolfpacksContainer.addItem(CreateWolfpackBox(pack),pack);
		 });
	  }
	
	function getProfileData() {		
		return {
			profile: userObj,
		  };
	}
	
	function geWolfpacksData() {
		return {
			wolfpacks: userObj
		  };
	}
	
	this.onReceiveName = function(nameHandler) {
		if(name != null) {
			nameHandler(name);
		} else {
			waitingForName.push(nameHandler);
		}
		
		return self;
	};
	
	return this;
};
var SearchApp = function(id,menu,applicationFrame,container) {
	var self = this;
	
	var menuList = menu.createNewMenuList("search","Searches");
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
	}).appendTo(this.frame);

	var searchBtn = $("<input/>").attr({
		"type" : "button",
		"value" : "Search"
	}).appendTo(this.frame).hide();
	
	function addSearchMenuItem(key,name) {
		var tempName;
		if(name == null) {
			tempName = "Search: "+key;
		} else {
			tempName = name;
		}
		
		menuList.addMenuItem(key,tempName);
		apps[key] = new Profile(key,name,applicationFrame)
			.onReceiveName(function(newName) {
				menuList.renameMenuItem(key,newName);
			});	
		
		eWolf.trigger("select",[key]);
	};
	
	function removeSearchMenuItem(key) {
		if(apps[key] != null) {
			apps[key].destroy();
			delete apps[key];
			apps[key] = null;
			menuList.removeMenuItem(key);
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
		
		if(key != null && key != "") {
			if(key == eWolf.data("userID") || apps[key] != null) {
				eWolf.trigger("select",[key]);
			} else {
				removeLastSearch();
				lastSearch = key;
				if(name == "") {
					name = null;
				}
				addSearchMenuItem(key,name);
			}			
		}
		
		return self;
	};
	
	searchBtn.click(function() {
		self.search(query.val());	
	});
	
	eWolf.bind("select."+id,function(event,eventId) {
		if(eventId != lastSearch && eventId != "__newmessage__"+lastSearch) {
			removeLastSearch();
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
	    
	    if(query.val() == "") {
	    	searchBtn.hide(200);
	    } else {
	    	searchBtn.show(200);
	    }
	});
		
	
	eWolf.bind("search",function(event,key,name) {
		self.search(key,name);
	});
	
	return this;
};var WolfpackPage = function (id,wolfpackName,applicationFrame) {
	var self = this;
	
	Application.call(this,id,applicationFrame);
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh();
				
	var topTitle = new TitleArea().appendTo(this.frame)
		.addFunction("Post", function() {
			new NewPost(id,applicationFrame,wolfpackName).select();
		});		
	
	if(wolfpackName != null) {
		topTitle.setTitle(CreateWolfpackBox(wolfpackName));
	} else {
		topTitle.setTitle("News Feed");
	}
	
	new WolfpackNewsFeedList(request,wolfpackName)
		.appendTo(this.frame);
	
	if(wolfpackName != null) {
		var addMembers = null;

		var members = new CommaSeperatedList("Members");
		topTitle.appendAtBottomPart(members.getList());
		
		this.showAddMembers = function () {
			members.hide(200);
			topTitle.hideFunction("Add members...");
			if(addMembers != null) {
				addMembers.remove();
			}
			
			addMembers = $("<span/>");
			topTitle.appendAtBottomPart(addMembers);
			
			new AddMembersToWolfpack(id,wolfpackName,members.getItemNames(),
					self.removeAddMemebers,request).frame.appendTo(addMembers);
		};
		
		this.removeAddMemebers = function () {
			if(addMembers != null) {
				addMembers.hide(200,function() {
					addMembers.remove();
					addMembers = null;
				});			
			}
			
			topTitle.showFunction("Add members...");
			members.show(200);
		};
		
		this.deleteWolfpack = function() {
			var diag = $("<div/>").attr({
				"id" : "dialog-confirm",
				"title" : "Delete wolfpack?"
			});
			
			var p = $("<p/>").appendTo(diag);
		
			p.append("The wolfpack will be permanently deleted and cannot be recovered. Are you sure?");
			
			diag.dialog({
				resizable: true,
				modal: true,
				buttons: {
					"Delete wolfpack": function() {
						// TODO: delete wolfpack
						$( this ).dialog( "close" );
						alert("Option unavailible");
					},
					Cancel: function() {
						$( this ).dialog( "close" );
					}
				}
			});
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
				members.addItem(CreateUserBox(member.id, member.name),member.name);
			});
		}
		
		request.register(getWolfpacksMembersData,
				new ResponseHandler("wolfpackMembers",
					["membersList"],
					handleWolfpacksMembersData).getHandler());
		
		topTitle.addFunction("Add members...", this.showAddMembers);		
		topTitle.addFunction("Delete wolfpack", this.deleteWolfpack);
		
		eWolf.bind("select."+id,function(event,eventId) {
			self.removeAddMemebers();
		});
	}
	
	this.getName = function() {
		return wolfpackName;			
	};
	
	return this;
};
