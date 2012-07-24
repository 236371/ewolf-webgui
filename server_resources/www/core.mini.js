var CommaSeperatedList = function(title) {
	var list = $("<span/>")
		.attr("class","wolfpacksBox")
		.append(title+": ")
		.hide();
	
	var items = null;
	
	this.addItem = function (item) {
		if(items == null) {
			items = $("<span/>").appendTo(list);
			list.show();
		} else {
			items.append(", ");
		}
		
		items.append(item);
	};
	
	this.removeAll = function() {
		if(items != null) {
			items.remove();
			items = null;
			list.hide();
		}		
	};
	
	this.getList = function () {
		return list;
	};
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


	return {
		listenToEvent : function(eventStart,eventEnd) {
			eWolf.bind(eventStart,startLoading);	
			eWolf.bind(eventEnd,stopLoading);
		},
		stopListenToEvent : function(eventStart,eventEnd) {
			eWolf.unbind(eventStart,startLoading);	
			eWolf.unbind(eventEnd,stopLoading);
		}
	};
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
};var BasicRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	
	var observersRequestFunction = [];
	var observersHandleDataFunction = [];
	var timer = null;
	
	function trigger() {
		eWolf.trigger('needRefresh.'+id,[id]);
	}
	
	function onPostComplete () {
		eWolf.trigger("loadingEnd",[id]);
		
		if(refreshIntervalSec > 0) {
			clearTimeout(timer);
			timer = setTimeout(trigger,refreshIntervalSec*1000);
		}
	}
	
	var _makeRequest = function(address,data,success) {
		// Not implemented
	};	

	
	return {		
		getId : function() {
			return id;
		},
		setRequestAddress : function(inputRequestAddress) {
			requestAddress = inputRequestAddress;
			return this;
		},
		_makeRequest: function (address,data,success) {
			// Not implemented
		},
		request: function(data,handleDataFunction) {
			clearTimeout(timer);
			eWolf.trigger("loading",[id]);
			
			this._makeRequest(requestAddress,data,
				function(receivedData,textStatus) {
					handleDataFunction(receivedData,textStatus,data);
				}).complete(onPostComplete);
			
			return this;
		},
		requestAll: function() {
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
			
			return this;
		},
		register: function(requestFunction,handleDataFunction) {
			if(requestFunction != null && handleDataFunction != null) {
				observersRequestFunction.push(requestFunction);
				observersHandleDataFunction.push(handleDataFunction);
			}
			
			return this;
		},
		listenToRefresh: function() {
			var req = this;
			eWolf.bind("refresh."+id,function(event,eventId) {
				if(id == eventId) {
					req.requestAll();
				}
			});
			
			return this;
		}
		
	};
};

var PostRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	var res = new BasicRequestHandler(id,requestAddress,refreshIntervalSec);
	
	res._makeRequest = function (address,data,success) {
		return $.post(address,JSON.stringify(data),success,"json");
	};
	
	return res;
};

var JSONRequestHandler = function(id,requestAddress,refreshIntervalSec) {
	var res = new BasicRequestHandler(id,requestAddress,refreshIntervalSec);
	
	res._makeRequest = function (address,data,success) {		
		return $.getJSON(address,data,success);
	};
	
	return res;
};var ResonseHandler = function(category, requiredFields, handler) {
	return function(data, textStatus, postData) {
		if (data[category] != null) {
			if (data[category].result == "success") {
				var valid = true;
				$.each(requiredFields, function(i, field) {
					if (field == null) {
						console.log("No field: \"" + field + "\" in response");
						valid = false;
						return false;
					}
				});

				if (valid) {
					handler(data[category], textStatus, postData);
				}
			} else {
				console.log("Response unsuccesssful: " + data[category].result);
			}

		} else {
			console.log("No category: \"" + category + "\" in response");
		}
	};
};var TitleArea = function (title) {
	var thisObj = this;
	
	var frame = $("<div/>").attr("class","titleArea");
	
	var topPart = $("<div/>").appendTo(frame);
	var bottomPart = $("<div/>").attr("class","titleBottomPart").appendTo(frame);
	
	var table = $("<table/>").attr("class","titleTable").appendTo(topPart);
	
	var row = $("<tr>").appendTo(table);
	var titleTextArea = $("<td>")
		.attr("class","titleTextArea").appendTo(row);
	var titleFunctionsArea = $("<td>")
		.attr("class","titleFunctionsArea").appendTo(row);
	
	var theTitle = $("<span/>").attr({
		"class" : "eWolfTitle"
	}).appendTo(titleTextArea);
	
	var functions = {};
	
	this.setTitle = function (newTitle) {
		if(newTitle != null) {
			theTitle.html(newTitle);
		}
	};
	
	this.setTitle(title);
	
	this.appendTo = function (container) {
		frame.appendTo(container);
		return thisObj;
	};
	
	this.appendAtTitleTextArea = function (obj) {
		titleTextArea.append(obj);
		return thisObj;
	};
	
	this.appendAtTitleFunctionsArea = function (obj) {
		titleFunctionsArea.append(obj);
		return thisObj;
	};
	
	this.appendAtBottomPart = function (obj) {
		bottomPart.append(obj);
		return thisObj;
	};
	
	this.addFunction = function (functionName,functionOp) {
		if(functions[functionName] == null) {
			functions[functionName] = $("<input/>").attr({
				"type": "button",
				"value": functionName
			}).click(functionOp).appendTo(titleFunctionsArea);
		}
		
		return this;
	};
	
	this.removeFunction = function (functionName) {
		if(functions[functionName] != null) {
			functions[functionName].remove();
			functions[functionName] = null;
		}
		
		return this;
	};
	
	return this;
};var User = function(id,name) {
	return $("<span/>").attr({
		"style": "width:1%;",
		"class": "selectableBox"
	}).text(name).click(function() {
		if(id != eWolf.data("userID")) {
			eWolf.trigger("search",[id,name]);
		} else {
			eWolf.trigger("select",[id]);
		}
	});
};var Wolfpack = function(name) {
	return $("<span/>").attr({
		"style": "width:1%;",
		"class": "selectableBox"
	}).text(name).click(function() {
		eWolf.trigger("select",["__pack__"+name]);
	});
};var Wolfpacks = function (menu,request,applicationFrame) {
	var obj = this;
	
	var wolfpacks = {};
	
	var menuList = menu.createNewMenuList("wolfpacks","Wolfpacks");
	
	request.register(function() {
		return {
			 wolfpacks:{}
		};
	},new ResonseHandler("wolfpacks",["wolfpacksList"],handleWolfpacks));
		
	this.addWolfpack = function (pack) {
		if(wolfpacks[pack] == null) {		
			if(pack != "wall-readers") {
				menuList.addMenuItem("__pack__"+pack,pack);
			}
			
			var app = new WolfpackPage("__pack__"+pack,pack,applicationFrame);
			
			
			wolfpacks[pack] = app;
		}		
	};
	
	this.removeWolfpack = function(pack) {
		if(wolfpacks[pack] != null) {
			menuList.removeMenuItem("__pack__"+pack);
			wolfpacks[pack].destroy();
			wolfpacks[pack] = null;
		}
	};
	
	function handleWolfpacks(data, textStatus, postData) {
		$.each(data.wolfpacksList,
				function(i,pack){
			obj.addWolfpack(pack);
		});
		
		eWolf.trigger("select",["__pack__wall-readers"]);
	}	
	
	return this;
};



var eWolfMaster = new function() {
};

var eWolf = $(eWolfMaster);

$(document).ready(function () {
	new Loading($("#loadingFrame"));
	
	eWolf.sideMenu = new SideMenu($("#menu"),$("#mainFrame"),$("#topbarID"));	
	eWolf.applicationFrame = $("#applicationFrame");	
	eWolf.mainApps = eWolf.sideMenu.createNewMenuList("mainapps","Main");
	
	getUserInformation();
});

function getUserInformation() {
	var request = new PostRequestHandler("eWolf","/json",0)
		.register(function() {
			return {
				profile: {}
			};
		},new ResonseHandler("profile",
					["id","name"],handleProfileData));
	
	eWolf.wolfpacks = new Wolfpacks(eWolf.sideMenu,request,eWolf.applicationFrame);
	request.requestAll();
	
	function handleProfileData(data, textStatus, postData) {
		document.title = "eWolf - " + data.name;
			
		eWolf.data('userID',data.id);
		eWolf.data('userName',data.name);
			
		createMainApps();			
	}	
}

function createMainApps() {
		
	eWolf.mainApps.addMenuItem(eWolf.data("userID"),"My Profile");
	new Profile(eWolf.data("userID"),eWolf.data('userName'),eWolf.applicationFrame);
	
	eWolf.mainApps.addMenuItem("__pack__wall-readers","News Feed");
	
	eWolf.mainApps.addMenuItem("messages","Messages");
	new Inbox("messages",eWolf.applicationFrame);
	
	new SearchApp("search",eWolf.sideMenu,eWolf.applicationFrame,
			$("#txtSearchBox"),$("#btnSearch"),$("#btnAdd"));
}/*
filedrag.js - HTML5 File Drag & Drop demonstration
Featured on SitePoint.com
Developed by Craig Buckler (@craigbuckler) of OptimalWorks.net
*/
var filedrag = function(uploaderArea) {
	
	var filesArray = [];

	var fileselect = $("<input/>").attr({
		"type": "file",
		"id": "fileselect",
		"name": "fileselect[]",
		"multiple": "multiple"
	}).appendTo(uploaderArea);
	
	var filedrag = $("<div/>")
		.attr("id","filedrag")
		.append("or drop files here")
		.appendTo(uploaderArea);
	
	var filelist = $("<ul/>")
		.attr({
			"id":"filelist",
			"class": "filesList"
		}).appendTo(uploaderArea);


	// output information
	function Output(msg) {
		var box = $("<input/>").attr({
			"type": "checkbox",
			"checked": "checked"
		});		

		$("<li/>").attr({
			"class": "filelistItem"
		}).append(box).append(msg).appendTo(filelist);
	}
	
	// call initialization file
	if (window.File && window.FileList && window.FileReader) {
		Init();
	}
		
	// getElementById
	function $id(id) {
		return document.getElementById(id);
	}

	// file drag hover
	function FileDragHover(e) {
		e.stopPropagation();
		e.preventDefault();
		e.target.className = (e.type == "dragover" ? "hover" : "");
	}


	// file selection
	function FileSelectHandler(e) {
		// cancel event and hover styling
		FileDragHover(e);

		// fetch FileList object
		var files = e.target.files || e.dataTransfer.files;

		// process all File objects
		for (var i = 0, f; f = files[i]; i++) {
			filesArray.push(f);
			ParseFile(f);
		}
	}

	// output file information
	function ParseFile(file) {
		Output(
			"<strong>" + file.name +
			"</strong> type: <strong>" + file.type +
			"</strong> size: <strong>" + file.size +
			"</strong> bytes"
		);
	}


	// initialize
	function Init() {
	
		// file select
		fileselect[0].addEventListener("change", FileSelectHandler, false);

		// is XHR2 available?
		var xhr = new XMLHttpRequest();
		if (xhr.upload) {

			// file drop
			filedrag[0].addEventListener("dragover", FileDragHover, false);
			filedrag[0].addEventListener("dragleave", FileDragHover, false);
			filedrag[0].addEventListener("drop", FileSelectHandler, false);
			filedrag[0].style.display = "block";
		}

	}
	
	return {
		getFiles: function() {
			return files;
		}
	};
};/**
 * http://github.com/valums/file-uploader
 * 
 * Multiple file upload component with progress-bar, drag-and-drop. 
 * © 2010 Andrew Valums ( andrew(at)valums.com ) 
 * 
 * Licensed under GNU GPL 2 or later and GNU LGPL 2 or later, see license.txt.
 */    

//
// Helper functions
//

var qq = qq || {};

/**
 * Adds all missing properties from second obj to first obj
 */ 
qq.extend = function(first, second){
    for (var prop in second){
        first[prop] = second[prop];
    }
};  

/**
 * Searches for a given element in the array, returns -1 if it is not present.
 * @param {Number} [from] The index at which to begin the search
 */
qq.indexOf = function(arr, elt, from){
    if (arr.indexOf) return arr.indexOf(elt, from);
    
    from = from || 0;
    var len = arr.length;    
    
    if (from < 0) from += len;  

    for (; from < len; from++){  
        if (from in arr && arr[from] === elt){  
            return from;
        }
    }  
    return -1;  
}; 
    
qq.getUniqueId = (function(){
    var id = 0;
    return function(){ return id++; };
})();

//
// Events

qq.attach = function(element, type, fn){
    if (element.addEventListener){
        element.addEventListener(type, fn, false);
    } else if (element.attachEvent){
        element.attachEvent('on' + type, fn);
    }
};
qq.detach = function(element, type, fn){
    if (element.removeEventListener){
        element.removeEventListener(type, fn, false);
    } else if (element.attachEvent){
        element.detachEvent('on' + type, fn);
    }
};

qq.preventDefault = function(e){
    if (e.preventDefault){
        e.preventDefault();
    } else{
        e.returnValue = false;
    }
};

//
// Node manipulations

/**
 * Insert node a before node b.
 */
qq.insertBefore = function(a, b){
    b.parentNode.insertBefore(a, b);
};
qq.remove = function(element){
    element.parentNode.removeChild(element);
};

qq.contains = function(parent, descendant){       
    // compareposition returns false in this case
    if (parent == descendant) return true;
    
    if (parent.contains){
        return parent.contains(descendant);
    } else {
        return !!(descendant.compareDocumentPosition(parent) & 8);
    }
};

/**
 * Creates and returns element from html string
 * Uses innerHTML to create an element
 */
qq.toElement = (function(){
    var div = document.createElement('div');
    return function(html){
        div.innerHTML = html;
        var element = div.firstChild;
        div.removeChild(element);
        return element;
    };
})();

//
// Node properties and attributes

/**
 * Sets styles for an element.
 * Fixes opacity in IE6-8.
 */
qq.css = function(element, styles){
    if (styles.opacity != null){
        if (typeof element.style.opacity != 'string' && typeof(element.filters) != 'undefined'){
            styles.filter = 'alpha(opacity=' + Math.round(100 * styles.opacity) + ')';
        }
    }
    qq.extend(element.style, styles);
};
qq.hasClass = function(element, name){
    var re = new RegExp('(^| )' + name + '( |$)');
    return re.test(element.className);
};
qq.addClass = function(element, name){
    if (!qq.hasClass(element, name)){
        element.className += ' ' + name;
    }
};
qq.removeClass = function(element, name){
    var re = new RegExp('(^| )' + name + '( |$)');
    element.className = element.className.replace(re, ' ').replace(/^\s+|\s+$/g, "");
};
qq.setText = function(element, text){
    element.innerText = text;
    element.textContent = text;
};

//
// Selecting elements

qq.children = function(element){
    var children = [],
    child = element.firstChild;

    while (child){
        if (child.nodeType == 1){
            children.push(child);
        }
        child = child.nextSibling;
    }

    return children;
};

qq.getByClass = function(element, className){
    if (element.querySelectorAll){
        return element.querySelectorAll('.' + className);
    }

    var result = [];
    var candidates = element.getElementsByTagName("*");
    var len = candidates.length;

    for (var i = 0; i < len; i++){
        if (qq.hasClass(candidates[i], className)){
            result.push(candidates[i]);
        }
    }
    return result;
};

/**
 * obj2url() takes a json-object as argument and generates
 * a querystring. pretty much like jQuery.param()
 * 
 * how to use:
 *
 *    `qq.obj2url({a:'b',c:'d'},'http://any.url/upload?otherParam=value');`
 *
 * will result in:
 *
 *    `http://any.url/upload?otherParam=value&a=b&c=d`
 *
 * @param  Object JSON-Object
 * @param  String current querystring-part
 * @return String encoded querystring
 */
qq.obj2url = function(obj, temp, prefixDone){
    var uristrings = [],
        prefix = '&',
        add = function(nextObj, i){
            var nextTemp = temp 
                ? (/\[\]$/.test(temp)) // prevent double-encoding
                   ? temp
                   : temp+'['+i+']'
                : i;
            if ((nextTemp != 'undefined') && (i != 'undefined')) {  
                uristrings.push(
                    (typeof nextObj === 'object') 
                        ? qq.obj2url(nextObj, nextTemp, true)
                        : (Object.prototype.toString.call(nextObj) === '[object Function]')
                            ? encodeURIComponent(nextTemp) + '=' + encodeURIComponent(nextObj())
                            : encodeURIComponent(nextTemp) + '=' + encodeURIComponent(nextObj)                                                          
                );
            }
        }; 

    if (!prefixDone && temp) {
      prefix = (/\?/.test(temp)) ? (/\?$/.test(temp)) ? '' : '&' : '?';
      uristrings.push(temp);
      uristrings.push(qq.obj2url(obj));
    } else if ((Object.prototype.toString.call(obj) === '[object Array]') && (typeof obj != 'undefined') ) {
        // we wont use a for-in-loop on an array (performance)
        for (var i = 0, len = obj.length; i < len; ++i){
            add(obj[i], i);
        }
    } else if ((typeof obj != 'undefined') && (obj !== null) && (typeof obj === "object")){
        // for anything else but a scalar, we will use for-in-loop
        for (var i in obj){
            add(obj[i], i);
        }
    } else {
        uristrings.push(encodeURIComponent(temp) + '=' + encodeURIComponent(obj));
    }

    return uristrings.join(prefix)
                     .replace(/^&/, '')
                     .replace(/%20/g, '+'); 
};

//
//
// Uploader Classes
//
//

var qq = qq || {};
    
/**
 * Creates upload button, validates upload, but doesn't create file list or dd. 
 */
qq.FileUploaderBasic = function(o){
    this._options = {
        // set to true to see the server response
        debug: false,
        action: '/server/upload',
        params: {},
        button: null,
        multiple: true,
        maxConnections: 3,
        // validation        
        allowedExtensions: [],               
        sizeLimit: 0,   
        minSizeLimit: 0,                             
        // events
        // return false to cancel submit
        onSubmit: function(id, fileName){},
        onProgress: function(id, fileName, loaded, total){},
        onComplete: function(id, fileName, responseJSON){},
        onCancel: function(id, fileName){},
        // messages                
        messages: {
            typeError: "{file} has invalid extension. Only {extensions} are allowed.",
            sizeError: "{file} is too large, maximum file size is {sizeLimit}.",
            minSizeError: "{file} is too small, minimum file size is {minSizeLimit}.",
            emptyError: "{file} is empty, please select files again without it.",
            onLeave: "The files are being uploaded, if you leave now the upload will be cancelled."            
        },
        showMessage: function(message){
            alert(message);
        }               
    };
    qq.extend(this._options, o);
        
    // number of files being uploaded
    this._filesInProgress = 0;
    this._handler = this._createUploadHandler(); 
    
    if (this._options.button){ 
        this._button = this._createUploadButton(this._options.button);
    }
                        
    this._preventLeaveInProgress();         
};
   
qq.FileUploaderBasic.prototype = {
    setParams: function(params){
        this._options.params = params;
    },
    getInProgress: function(){
        return this._filesInProgress;         
    },
    _createUploadButton: function(element){
        var self = this;
        
        return new qq.UploadButton({
            element: element,
            multiple: this._options.multiple && qq.UploadHandlerXhr.isSupported(),
            onChange: function(input){
                self._onInputChange(input);
            }        
        });           
    },    
    _createUploadHandler: function(){
        var self = this,
            handlerClass;        
        
        if(qq.UploadHandlerXhr.isSupported()){           
            handlerClass = 'UploadHandlerXhr';                        
        } else {
            handlerClass = 'UploadHandlerForm';
        }

        var handler = new qq[handlerClass]({
            debug: this._options.debug,
            action: this._options.action,         
            maxConnections: this._options.maxConnections,   
            onProgress: function(id, fileName, loaded, total){                
                self._onProgress(id, fileName, loaded, total);
                self._options.onProgress(id, fileName, loaded, total);                    
            },            
            onComplete: function(id, fileName, result){
                self._onComplete(id, fileName, result);
                self._options.onComplete(id, fileName, result);
            },
            onCancel: function(id, fileName){
                self._onCancel(id, fileName);
                self._options.onCancel(id, fileName);
            }
        });

        return handler;
    },    
    _preventLeaveInProgress: function(){
        var self = this;
        
        qq.attach(window, 'beforeunload', function(e){
            if (!self._filesInProgress){return;}
            
            var e = e || window.event;
            // for ie, ff
            e.returnValue = self._options.messages.onLeave;
            // for webkit
            return self._options.messages.onLeave;             
        });        
    },    
    _onSubmit: function(id, fileName){
        this._filesInProgress++;  
    },
    _onProgress: function(id, fileName, loaded, total){        
    },
    _onComplete: function(id, fileName, result){
        this._filesInProgress--;                 
        if (result.error){
            this._options.showMessage(result.error);
        }             
    },
    _onCancel: function(id, fileName){
        this._filesInProgress--;        
    },
    _onInputChange: function(input){
        if (this._handler instanceof qq.UploadHandlerXhr){                
            this._uploadFileList(input.files);                   
        } else {             
            if (this._validateFile(input)){                
                this._uploadFile(input);                                    
            }                      
        }               
        this._button.reset();   
    },  
    _uploadFileList: function(files){
        for (var i=0; i<files.length; i++){
            if ( !this._validateFile(files[i])){
                return;
            }            
        }
        
        for (var i=0; i<files.length; i++){
            this._uploadFile(files[i]);        
        }        
    },       
    _uploadFile: function(fileContainer){      
        var id = this._handler.add(fileContainer);
        var fileName = this._handler.getName(id);
        
        if (this._options.onSubmit(id, fileName) !== false){
            this._onSubmit(id, fileName);
            this._handler.upload(id, this._options.params);
        }
    },      
    _validateFile: function(file){
        var name, size;
        
        if (file.value){
            // it is a file input            
            // get input value and remove path to normalize
            name = file.value.replace(/.*(\/|\\)/, "");
        } else {
            // fix missing properties in Safari
            name = file.fileName != null ? file.fileName : file.name;
            size = file.fileSize != null ? file.fileSize : file.size;
        }
                    
        if (! this._isAllowedExtension(name)){            
            this._error('typeError', name);
            return false;
            
        } else if (size === 0){            
            this._error('emptyError', name);
            return false;
                                                     
        } else if (size && this._options.sizeLimit && size > this._options.sizeLimit){            
            this._error('sizeError', name);
            return false;
                        
        } else if (size && size < this._options.minSizeLimit){
            this._error('minSizeError', name);
            return false;            
        }
        
        return true;                
    },
    _error: function(code, fileName){
        var message = this._options.messages[code];        
        function r(name, replacement){ message = message.replace(name, replacement); }
        
        r('{file}', this._formatFileName(fileName));        
        r('{extensions}', this._options.allowedExtensions.join(', '));
        r('{sizeLimit}', this._formatSize(this._options.sizeLimit));
        r('{minSizeLimit}', this._formatSize(this._options.minSizeLimit));
        
        this._options.showMessage(message);                
    },
    _formatFileName: function(name){
        if (name.length > 33){
            name = name.slice(0, 19) + '...' + name.slice(-13);    
        }
        return name;
    },
    _isAllowedExtension: function(fileName){
        var ext = (-1 !== fileName.indexOf('.')) ? fileName.replace(/.*[.]/, '').toLowerCase() : '';
        var allowed = this._options.allowedExtensions;
        
        if (!allowed.length){return true;}        
        
        for (var i=0; i<allowed.length; i++){
            if (allowed[i].toLowerCase() == ext){ return true;}    
        }
        
        return false;
    },    
    _formatSize: function(bytes){
        var i = -1;                                    
        do {
            bytes = bytes / 1024;
            i++;  
        } while (bytes > 99);
        
        return Math.max(bytes, 0.1).toFixed(1) + ['kB', 'MB', 'GB', 'TB', 'PB', 'EB'][i];          
    }
};
    
       
/**
 * Class that creates upload widget with drag-and-drop and file list
 * @inherits qq.FileUploaderBasic
 */
qq.FileUploader = function(o){
    // call parent constructor
    qq.FileUploaderBasic.apply(this, arguments);
    
    // additional options    
    qq.extend(this._options, {
        element: null,
        // if set, will be used instead of qq-upload-list in template
        listElement: null,
                
        template: '<div class="qq-uploader">' + 
                '<div class="qq-upload-drop-area"><span>Drop files here to upload</span></div>' +
                '<div class="qq-upload-button">Upload a file</div>' +
                '<ul class="qq-upload-list"></ul>' + 
             '</div>',

        // template for one item in file list
        fileTemplate: '<li>' +
                '<span class="qq-upload-file"></span>' +
                '<span class="qq-upload-spinner"></span>' +
                '<span class="qq-upload-size"></span>' +
                '<a class="qq-upload-cancel" href="#">Cancel</a>' +
                '<span class="qq-upload-failed-text">Failed</span>' +
            '</li>',        
        
        classes: {
            // used to get elements from templates
            button: 'qq-upload-button',
            drop: 'qq-upload-drop-area',
            dropActive: 'qq-upload-drop-area-active',
            list: 'qq-upload-list',
                        
            file: 'qq-upload-file',
            spinner: 'qq-upload-spinner',
            size: 'qq-upload-size',
            cancel: 'qq-upload-cancel',

            // added to list item when upload completes
            // used in css to hide progress spinner
            success: 'qq-upload-success',
            fail: 'qq-upload-fail'
        }
    });
    // overwrite options with user supplied    
    qq.extend(this._options, o);       

    this._element = this._options.element;
    this._element.innerHTML = this._options.template;        
    this._listElement = this._options.listElement || this._find(this._element, 'list');
    
    this._classes = this._options.classes;
        
    this._button = this._createUploadButton(this._find(this._element, 'button'));        
    
    this._bindCancelEvent();
    this._setupDragDrop();
};

// inherit from Basic Uploader
qq.extend(qq.FileUploader.prototype, qq.FileUploaderBasic.prototype);

qq.extend(qq.FileUploader.prototype, {
    /**
     * Gets one of the elements listed in this._options.classes
     **/
    _find: function(parent, type){                                
        var element = qq.getByClass(parent, this._options.classes[type])[0];        
        if (!element){
            throw new Error('element not found ' + type);
        }
        
        return element;
    },
    _setupDragDrop: function(){
        var self = this,
            dropArea = this._find(this._element, 'drop');                        

        var dz = new qq.UploadDropZone({
            element: dropArea,
            onEnter: function(e){
                qq.addClass(dropArea, self._classes.dropActive);
                e.stopPropagation();
            },
            onLeave: function(e){
                e.stopPropagation();
            },
            onLeaveNotDescendants: function(e){
                qq.removeClass(dropArea, self._classes.dropActive);  
            },
            onDrop: function(e){
                dropArea.style.display = 'none';
                qq.removeClass(dropArea, self._classes.dropActive);
                self._uploadFileList(e.dataTransfer.files);    
            }
        });
                
        dropArea.style.display = 'none';

        qq.attach(document, 'dragenter', function(e){     
            if (!dz._isValidFileDrag(e)) return; 
            
            dropArea.style.display = 'block';            
        });                 
        qq.attach(document, 'dragleave', function(e){
            if (!dz._isValidFileDrag(e)) return;            
            
            var relatedTarget = document.elementFromPoint(e.clientX, e.clientY);
            // only fire when leaving document out
            if ( ! relatedTarget || relatedTarget.nodeName == "HTML"){               
                dropArea.style.display = 'none';                                            
            }
        });                
    },
    _onSubmit: function(id, fileName){
        qq.FileUploaderBasic.prototype._onSubmit.apply(this, arguments);
        this._addToList(id, fileName);  
    },
    _onProgress: function(id, fileName, loaded, total){
        qq.FileUploaderBasic.prototype._onProgress.apply(this, arguments);

        var item = this._getItemByFileId(id);
        var size = this._find(item, 'size');
        size.style.display = 'inline';
        
        var text; 
        if (loaded != total){
            text = Math.round(loaded / total * 100) + '% from ' + this._formatSize(total);
        } else {                                   
            text = this._formatSize(total);
        }          
        
        qq.setText(size, text);         
    },
    _onComplete: function(id, fileName, result){
        qq.FileUploaderBasic.prototype._onComplete.apply(this, arguments);

        // mark completed
        var item = this._getItemByFileId(id);                
        qq.remove(this._find(item, 'cancel'));
        qq.remove(this._find(item, 'spinner'));
        
        if (result.success){
            qq.addClass(item, this._classes.success);    
        } else {
            qq.addClass(item, this._classes.fail);
        }         
    },
    _addToList: function(id, fileName){
        var item = qq.toElement(this._options.fileTemplate);                
        item.qqFileId = id;

        var fileElement = this._find(item, 'file');        
        qq.setText(fileElement, this._formatFileName(fileName));
        this._find(item, 'size').style.display = 'none';        

        this._listElement.appendChild(item);
    },
    _getItemByFileId: function(id){
        var item = this._listElement.firstChild;        
        
        // there can't be txt nodes in dynamically created list
        // and we can  use nextSibling
        while (item){            
            if (item.qqFileId == id) return item;            
            item = item.nextSibling;
        }          
    },
    /**
     * delegate click event for cancel link 
     **/
    _bindCancelEvent: function(){
        var self = this,
            list = this._listElement;            
        
        qq.attach(list, 'click', function(e){            
            e = e || window.event;
            var target = e.target || e.srcElement;
            
            if (qq.hasClass(target, self._classes.cancel)){                
                qq.preventDefault(e);
               
                var item = target.parentNode;
                self._handler.cancel(item.qqFileId);
                qq.remove(item);
            }
        });
    }    
});
    
qq.UploadDropZone = function(o){
    this._options = {
        element: null,  
        onEnter: function(e){},
        onLeave: function(e){},  
        // is not fired when leaving element by hovering descendants   
        onLeaveNotDescendants: function(e){},   
        onDrop: function(e){}                       
    };
    qq.extend(this._options, o); 
    
    this._element = this._options.element;
    
    this._disableDropOutside();
    this._attachEvents();   
};

qq.UploadDropZone.prototype = {
    _disableDropOutside: function(e){
        // run only once for all instances
        if (!qq.UploadDropZone.dropOutsideDisabled ){

            qq.attach(document, 'dragover', function(e){
                if (e.dataTransfer){
                    e.dataTransfer.dropEffect = 'none';
                    e.preventDefault(); 
                }           
            });
            
            qq.UploadDropZone.dropOutsideDisabled = true; 
        }        
    },
    _attachEvents: function(){
        var self = this;              
                  
        qq.attach(self._element, 'dragover', function(e){
            if (!self._isValidFileDrag(e)) return;
            
            var effect = e.dataTransfer.effectAllowed;
            if (effect == 'move' || effect == 'linkMove'){
                e.dataTransfer.dropEffect = 'move'; // for FF (only move allowed)    
            } else {                    
                e.dataTransfer.dropEffect = 'copy'; // for Chrome
            }
                                                     
            e.stopPropagation();
            e.preventDefault();                                                                    
        });
        
        qq.attach(self._element, 'dragenter', function(e){
            if (!self._isValidFileDrag(e)) return;
                        
            self._options.onEnter(e);
        });
        
        qq.attach(self._element, 'dragleave', function(e){
            if (!self._isValidFileDrag(e)) return;
            
            self._options.onLeave(e);
            
            var relatedTarget = document.elementFromPoint(e.clientX, e.clientY);                      
            // do not fire when moving a mouse over a descendant
            if (qq.contains(this, relatedTarget)) return;
                        
            self._options.onLeaveNotDescendants(e); 
        });
                
        qq.attach(self._element, 'drop', function(e){
            if (!self._isValidFileDrag(e)) return;
            
            e.preventDefault();
            self._options.onDrop(e);
        });          
    },
    _isValidFileDrag: function(e){
        var dt = e.dataTransfer,
            // do not check dt.types.contains in webkit, because it crashes safari 4            
            isWebkit = navigator.userAgent.indexOf("AppleWebKit") > -1;                        

        // dt.effectAllowed is none in Safari 5
        // dt.types.contains check is for firefox            
        return dt && dt.effectAllowed != 'none' && 
            (dt.files || (!isWebkit && dt.types.contains && dt.types.contains('Files')));
        
    }        
}; 

qq.UploadButton = function(o){
    this._options = {
        element: null,  
        // if set to true adds multiple attribute to file input      
        multiple: false,
        // name attribute of file input
        name: 'file',
        onChange: function(input){},
        hoverClass: 'qq-upload-button-hover',
        focusClass: 'qq-upload-button-focus'                       
    };
    
    qq.extend(this._options, o);
        
    this._element = this._options.element;
    
    // make button suitable container for input
    qq.css(this._element, {
        position: 'relative',
        overflow: 'hidden',
        // Make sure browse button is in the right side
        // in Internet Explorer
        direction: 'ltr'
    });   
    
    this._input = this._createInput();
};

qq.UploadButton.prototype = {
    /* returns file input element */    
    getInput: function(){
        return this._input;
    },
    /* cleans/recreates the file input */
    reset: function(){
        if (this._input.parentNode){
            qq.remove(this._input);    
        }                
        
        qq.removeClass(this._element, this._options.focusClass);
        this._input = this._createInput();
    },    
    _createInput: function(){                
        var input = document.createElement("input");
        
        if (this._options.multiple){
            input.setAttribute("multiple", "multiple");
        }
                
        input.setAttribute("type", "file");
        input.setAttribute("name", this._options.name);
        
        qq.css(input, {
            position: 'absolute',
            // in Opera only 'browse' button
            // is clickable and it is located at
            // the right side of the input
            right: 0,
            top: 0,
            fontFamily: 'Arial',
            // 4 persons reported this, the max values that worked for them were 243, 236, 236, 118
            fontSize: '118px',
            margin: 0,
            padding: 0,
            cursor: 'pointer',
            opacity: 0
        });
        
        this._element.appendChild(input);

        var self = this;
        qq.attach(input, 'change', function(){
            self._options.onChange(input);
        });
                
        qq.attach(input, 'mouseover', function(){
            qq.addClass(self._element, self._options.hoverClass);
        });
        qq.attach(input, 'mouseout', function(){
            qq.removeClass(self._element, self._options.hoverClass);
        });
        qq.attach(input, 'focus', function(){
            qq.addClass(self._element, self._options.focusClass);
        });
        qq.attach(input, 'blur', function(){
            qq.removeClass(self._element, self._options.focusClass);
        });

        // IE and Opera, unfortunately have 2 tab stops on file input
        // which is unacceptable in our case, disable keyboard access
        if (window.attachEvent){
            // it is IE or Opera
            input.setAttribute('tabIndex', "-1");
        }

        return input;            
    }        
};

/**
 * Class for uploading files, uploading itself is handled by child classes
 */
qq.UploadHandlerAbstract = function(o){
    this._options = {
        debug: false,
        action: '/upload.php',
        // maximum number of concurrent uploads        
        maxConnections: 999,
        onProgress: function(id, fileName, loaded, total){},
        onComplete: function(id, fileName, response){},
        onCancel: function(id, fileName){}
    };
    qq.extend(this._options, o);    
    
    this._queue = [];
    // params for files in queue
    this._params = [];
};
qq.UploadHandlerAbstract.prototype = {
    log: function(str){
        if (this._options.debug && window.console) console.log('[uploader] ' + str);        
    },
    /**
     * Adds file or file input to the queue
     * @returns id
     **/    
    add: function(file){},
    /**
     * Sends the file identified by id and additional query params to the server
     */
    upload: function(id, params){
        var len = this._queue.push(id);

        var copy = {};        
        qq.extend(copy, params);
        this._params[id] = copy;        
                
        // if too many active uploads, wait...
        if (len <= this._options.maxConnections){               
            this._upload(id, this._params[id]);
        }
    },
    /**
     * Cancels file upload by id
     */
    cancel: function(id){
        this._cancel(id);
        this._dequeue(id);
    },
    /**
     * Cancells all uploads
     */
    cancelAll: function(){
        for (var i=0; i<this._queue.length; i++){
            this._cancel(this._queue[i]);
        }
        this._queue = [];
    },
    /**
     * Returns name of the file identified by id
     */
    getName: function(id){},
    /**
     * Returns size of the file identified by id
     */          
    getSize: function(id){},
    /**
     * Returns id of files being uploaded or
     * waiting for their turn
     */
    getQueue: function(){
        return this._queue;
    },
    /**
     * Actual upload method
     */
    _upload: function(id){},
    /**
     * Actual cancel method
     */
    _cancel: function(id){},     
    /**
     * Removes element from queue, starts upload of next
     */
    _dequeue: function(id){
        var i = qq.indexOf(this._queue, id);
        this._queue.splice(i, 1);
                
        var max = this._options.maxConnections;
        
        if (this._queue.length >= max && i < max){
            var nextId = this._queue[max-1];
            this._upload(nextId, this._params[nextId]);
        }
    }        
};

/**
 * Class for uploading files using form and iframe
 * @inherits qq.UploadHandlerAbstract
 */
qq.UploadHandlerForm = function(o){
    qq.UploadHandlerAbstract.apply(this, arguments);
       
    this._inputs = {};
};
// @inherits qq.UploadHandlerAbstract
qq.extend(qq.UploadHandlerForm.prototype, qq.UploadHandlerAbstract.prototype);

qq.extend(qq.UploadHandlerForm.prototype, {
    add: function(fileInput){
        fileInput.setAttribute('name', 'qqfile');
        var id = 'qq-upload-handler-iframe' + qq.getUniqueId();       
        
        this._inputs[id] = fileInput;
        
        // remove file input from DOM
        if (fileInput.parentNode){
            qq.remove(fileInput);
        }
                
        return id;
    },
    getName: function(id){
        // get input value and remove path to normalize
        return this._inputs[id].value.replace(/.*(\/|\\)/, "");
    },    
    _cancel: function(id){
        this._options.onCancel(id, this.getName(id));
        
        delete this._inputs[id];        

        var iframe = document.getElementById(id);
        if (iframe){
            // to cancel request set src to something else
            // we use src="javascript:false;" because it doesn't
            // trigger ie6 prompt on https
            iframe.setAttribute('src', 'javascript:false;');

            qq.remove(iframe);
        }
    },     
    _upload: function(id, params){                        
        var input = this._inputs[id];
        
        if (!input){
            throw new Error('file with passed id was not added, or already uploaded or cancelled');
        }                

        var fileName = this.getName(id);
                
        var iframe = this._createIframe(id);
        var form = this._createForm(iframe, params);
        form.appendChild(input);

        var self = this;
        this._attachLoadEvent(iframe, function(){                                 
            self.log('iframe loaded');
            
            var response = self._getIframeContentJSON(iframe);

            self._options.onComplete(id, fileName, response);
            self._dequeue(id);
            
            delete self._inputs[id];
            // timeout added to fix busy state in FF3.6
            setTimeout(function(){
                qq.remove(iframe);
            }, 1);
        });

        form.submit();        
        qq.remove(form);        
        
        return id;
    }, 
    _attachLoadEvent: function(iframe, callback){
        qq.attach(iframe, 'load', function(){
            // when we remove iframe from dom
            // the request stops, but in IE load
            // event fires
            if (!iframe.parentNode){
                return;
            }

            // fixing Opera 10.53
            if (iframe.contentDocument &&
                iframe.contentDocument.body &&
                iframe.contentDocument.body.innerHTML == "false"){
                // In Opera event is fired second time
                // when body.innerHTML changed from false
                // to server response approx. after 1 sec
                // when we upload file with iframe
                return;
            }

            callback();
        });
    },
    /**
     * Returns json object received by iframe from server.
     */
    _getIframeContentJSON: function(iframe){
        // iframe.contentWindow.document - for IE<7
        var doc = iframe.contentDocument ? iframe.contentDocument: iframe.contentWindow.document,
            response;
        
        this.log("converting iframe's innerHTML to JSON");
        this.log("innerHTML = " + doc.body.innerHTML);
                        
        try {
            response = eval("(" + doc.body.innerHTML + ")");
        } catch(err){
            response = {};
        }        

        return response;
    },
    /**
     * Creates iframe with unique name
     */
    _createIframe: function(id){
        // We can't use following code as the name attribute
        // won't be properly registered in IE6, and new window
        // on form submit will open
        // var iframe = document.createElement('iframe');
        // iframe.setAttribute('name', id);

        var iframe = qq.toElement('<iframe src="javascript:false;" name="' + id + '" />');
        // src="javascript:false;" removes ie6 prompt on https

        iframe.setAttribute('id', id);

        iframe.style.display = 'none';
        document.body.appendChild(iframe);

        return iframe;
    },
    /**
     * Creates form, that will be submitted to iframe
     */
    _createForm: function(iframe, params){
        // We can't use the following code in IE6
        // var form = document.createElement('form');
        // form.setAttribute('method', 'post');
        // form.setAttribute('enctype', 'multipart/form-data');
        // Because in this case file won't be attached to request
        var form = qq.toElement('<form method="post" enctype="multipart/form-data"></form>');

        var queryString = qq.obj2url(params, this._options.action);

        form.setAttribute('action', queryString);
        form.setAttribute('target', iframe.name);
        form.style.display = 'none';
        document.body.appendChild(form);

        return form;
    }
});

/**
 * Class for uploading files using xhr
 * @inherits qq.UploadHandlerAbstract
 */
qq.UploadHandlerXhr = function(o){
    qq.UploadHandlerAbstract.apply(this, arguments);

    this._files = [];
    this._xhrs = [];
    
    // current loaded size in bytes for each file 
    this._loaded = [];
};

// static method
qq.UploadHandlerXhr.isSupported = function(){
    var input = document.createElement('input');
    input.type = 'file';        
    
    return (
        'multiple' in input &&
        typeof File != "undefined" &&
        typeof (new XMLHttpRequest()).upload != "undefined" );       
};

// @inherits qq.UploadHandlerAbstract
qq.extend(qq.UploadHandlerXhr.prototype, qq.UploadHandlerAbstract.prototype)

qq.extend(qq.UploadHandlerXhr.prototype, {
    /**
     * Adds file to the queue
     * Returns id to use with upload, cancel
     **/    
    add: function(file){
        if (!(file instanceof File)){
            throw new Error('Passed obj in not a File (in qq.UploadHandlerXhr)');
        }
                
        return this._files.push(file) - 1;        
    },
    getName: function(id){        
        var file = this._files[id];
        // fix missing name in Safari 4
        return file.fileName != null ? file.fileName : file.name;       
    },
    getSize: function(id){
        var file = this._files[id];
        return file.fileSize != null ? file.fileSize : file.size;
    },    
    /**
     * Returns uploaded bytes for file identified by id 
     */    
    getLoaded: function(id){
        return this._loaded[id] || 0; 
    },
    /**
     * Sends the file identified by id and additional query params to the server
     * @param {Object} params name-value string pairs
     */    
    _upload: function(id, params){
        var file = this._files[id],
            name = this.getName(id),
            size = this.getSize(id);
                
        this._loaded[id] = 0;
                                
        var xhr = this._xhrs[id] = new XMLHttpRequest();
        var self = this;
                                        
        xhr.upload.onprogress = function(e){
            if (e.lengthComputable){
                self._loaded[id] = e.loaded;
                self._options.onProgress(id, name, e.loaded, e.total);
            }
        };

        xhr.onreadystatechange = function(){            
            if (xhr.readyState == 4){
                self._onComplete(id, xhr);                    
            }
        };

        // build query string
        params = params || {};
        params['qqfile'] = name;
        var queryString = qq.obj2url(params, this._options.action);

        xhr.open("POST", queryString, true);
        xhr.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        xhr.setRequestHeader("X-File-Name", encodeURIComponent(name));
        xhr.setRequestHeader("Content-Type", "application/octet-stream");
        xhr.send(file);
    },
    _onComplete: function(id, xhr){
        // the request was aborted/cancelled
        if (!this._files[id]) return;
        
        var name = this.getName(id);
        var size = this.getSize(id);
        
        this._options.onProgress(id, name, size, size);
                
        if (xhr.status == 200){
            this.log("xhr - server response received");
            this.log("responseText = " + xhr.responseText);
                        
            var response;
                    
            try {
                response = eval("(" + xhr.responseText + ")");
            } catch(err){
                response = {};
            }
            
            this._options.onComplete(id, name, response);
                        
        } else {                   
            this._options.onComplete(id, name, {});
        }
                
        this._files[id] = null;
        this._xhrs[id] = null;    
        this._dequeue(id);                    
    },
    _cancel: function(id){
        this._options.onCancel(id, this.getName(id));
        
        this._files[id] = null;
        
        if (this._xhrs[id]){
            this._xhrs[id].abort();
            this._xhrs[id] = null;                                   
        }
    }
});/**
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
	var isLoading = false;
	var selected = false;	
	var message = new MenuMessage(id+"Message","menuItemMessageClass",
			messageText,topbarFrame);
	
	var listItem = $("<li/>").attr({"id": id});
		
	var aObj = $("<a/>").appendTo(listItem);
	
	var titleBox = $("<span/>").attr({
		"id": id,
		"style": "width:1%;"
	}).appendTo(aObj);
	
	var refreshContainer = $("<div/>").attr({
		"class": "refreshButtonArea",
		"id": id,
	})	.appendTo(aObj).hide();
	
	var loadingContainer = $("<div/>").attr({
		"class": "refreshButtonArea",
		"id": id,
	})	.appendTo(aObj).hide();
	
	var refresh = $("<img/>").attr({
		"id": id,
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
			eWolf.trigger("refresh."+id,[id]);
		}	
	});
	
	listItem.mouseover(function() {
		message.show();
	});

	listItem.mouseout(function() {
		message.hide();
	});
	
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
	
	return {
		appendTo: function(place) {
			listItem.appendTo(place);
			updateView();
			return this;
		},
		getId : function() {
			return id;
		},
		renameTitle : function(newTitle) {
			title = newTitle;
			updateView();
		},
		destroy: function() {
			message.destroy();
			listItem.remove();
			delete this;
		}
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
		};var MenuList = function(menu,id,title,topbarFrame) {
	var items = [];
	
	var frame = $("<div/>").attr({
		"class" : "menuList",
		"id" : id+"Frame"
	}).hide();
	
	$("<div/>").attr({
		"class" : "menuListTitle",
		"id" : id+"Title"
	})	.append(title)
		.appendTo(frame);

	var list = $("<ul/>").attr({
		"id" : id
	})	.appendTo(frame);
	
	return {
		addMenuItem : function(id,title) {
			if(items[id] == null) {
				var menuItem = new MenuItem(id,title,
						"Click to show "+title.toLowerCase(),topbarFrame).appendTo(list);
				
				items[id] = menuItem;
				
				if(Object.keys(items).length > 0) {
					frame.show();
				}
			} else {
				console.log("[Menu Error] Item with id: "+ id +" already exist");
			}
			
		},
		removeMenuItem: function(removeId) {
			if(items[removeId] != null) {
				items[removeId].destroy();
				delete items[removeId];
			}
			
			if(Object.keys(items).length <= 0) {
				frame.hide();
			}
		},
		renameMenuItem : function(id,newTitle) {
			if(items[id] != null) {
				items[id].renameTitle(newTitle);
			}
		},
		appendTo : function(container) {
			frame.appendTo(container);
			return this;
		}
	};
};var MenuMessage = function(id,itemclass,text,container) {
	var message = null;
	
	return {
		show : function() {
			if(message == null) {
				message = $("<div/>").attr({
					"id": id,
					"class": itemclass
				}).text(text).appendTo(container);
			} else {
				message.show();
			}
		},
		hide : function() {
			if(message != null) {
				message.remove();
				message = null;
			}
		},
		destroy : function() {
			if(message != null) {
				message.remove();
				message = null;
				delete this;
			}
		}
	};
};

var SideMenu = function(menu, mainFrame,topbarFrame) {
	menu.data('menuObj', this);
	mainFrame.data('menuObj', this);
	var toggleButton = menu.children("#toggleButtons");

	var hideBtn = toggleButton.children("#btnHideMenu")
			.data('menuObj', this).click(function() {
				hideMenu();
			}),
		showBtn = toggleButton.children("#btnShowMenu")
			.data('menuObj', this).click(function() {
				showMenu();
			}),
		pinBtn = toggleButton.children("#btnPin")
			.data('menuObj', this).click(function() {
				pinMenu();
			}),
		unpinBtn = toggleButton.children("#btnUnPin")
			.data('menuObj', this).click(function() {
				unpinMenu();
			}),

		menuLists = [];
	
	function showMenu() {
		showBtn.hide();
		menuIn();
		mainFrameShrink();
		hideBtn.show();
	};

	function hideMenu() {
		hideBtn.hide();
		menuOut();
		mainFrameGrow();
		showBtn.show();
	};

	function pinMenu() {
		mainFrameShrink();
		pinBtn.hide();
		unpinBtn.show();
		hideBtn.show();
		menu.unbind("mouseover");
		menu.unbind("mouseout");
	};

	function unpinMenu() {
		mainFrameGrow();
		unpinBtn.hide();
		pinBtn.show();
		hideBtn.hide();
		menu.mouseover(function() {
			menuIn();
		});

		menu.mouseout(function() {
			menuOut();
		});
	};

	function menuOut() {
		menu.stop();

		menu.animate({
			opacity : 0.25,
			left : '-175px',
		}, 200, function() {
			// Animation complete.
		});
	};

	function menuIn() {
		menu.stop();

		menu.animate({
			opacity : 0.7,
			left : '-35px',
		}, 200, function() {
			// Animation complete.
		});
	};

	function mainFrameGrow() {
		mainFrame.stop();

		mainFrame.animate({
			left : '30px',
			right : '0'
		}, 200, function() {
			eWolf.trigger("mainFrameResize",["sideMenu"]);
		});
	};

	function mainFrameShrink() {
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
	
	return {
		append : function(item) {
			menu.append(item);
		},
		createNewMenuList : function(id, title) {
			var menuLst = new MenuList(this,id,title,topbarFrame)
							.appendTo(menu);
			menuLists.push(menuLst);
			return menuLst;
		}
	};
};var GenericItem = function(senderID,senderName,timestamp,mail,
		listClass,msgBoxClass,preMessageTitle,allowShrink) {
		
	var itemSpan = $("<span/>");
	
	var listItem = $("<li/>").attr({
		"class": listClass
	}).appendTo(itemSpan);
	
	var preMessageBox = $("<span/>").attr({
		"style": "width:1%;",
		"class": "preMessageBox"
	}).append(preMessageTitle).appendTo(listItem);	
	
	var isOnSender = false;
	var senderBox = User(senderID,senderName)
		.appendTo(listItem)
		.hover(function() {
			isOnSender = true;
		}, function () {
			isOnSender = false;
		});
	
	
	var timestampBox = TimestampBox(timestamp).appendTo(listItem);
		
	var itsMessage = $("<li/>").attr({
		 "class": msgBoxClass
	 })	.append(MailItem(JSON.parse(mail)))
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
	
	eWolf.bind("mainFrameResize", function(event,eventId) {
		updateView();
	});
	
	return {
		appendTo: function(place) {
			itemSpan.appendTo(place);
			updateView();
			return this;
		},
		prependTo: function(place) {
			itemSpan.prependTo(place);
			updateView();
			return this;
		},
		insertAfter: function(place) {
			itemSpan.insertAfter(place);
			updateView();
			return this;
		},
		getListItem : function() {
			return itemSpan;
		},
		destroy: function() {
			message.destroy();
			itemSpan.remove();
			delete this;
		}
	};
	
	return this;
};var GenericMailList = function(mailType,request,serverSettings,
		listClass,msgBoxClass,preMessageTitle,allowShrink) {
	var obj = this;
	
	var newestDate = null;
	var oldestDate = null;
	
	var lastItem = null;
	
	var frame = $("<span/>");
	
	var list = $("<ul/>").attr({
		"class" : "messageList"
	}).appendTo(frame);
	
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
	
	this.appendTo = function (canvas) {
		frame.appendTo(canvas);
		return this;
	};

	var responseHandler = new ResonseHandler(mailType,
			["mailList"],handleNewData);
	request.register(this.updateFromServer ,responseHandler);
	
	var showMore = new ShowMore(frame,function() {
		request.request(obj.updateFromServer (true),responseHandler);
	}).draw();
	
	function handleNewData(data, textStatus, parameters) {
		$.each(data.mailList, function(j, mailItem) {
			obj.addItem(mailItem.senderID,mailItem.senderName,
					mailItem.timestamp, mailItem.mail);
		});
		
		if (parameters[mailType].newerThan == null &&
				data.mailList.length < parameters[mailType].maxMessages) {
			showMore.remove();
		}
	}	
	
	return this;
};

var NewsFeedList = function (request,serverSettings) {
	$.extend(serverSettings,{maxMessages:2});
	
	return new GenericMailList("newsFeed",request,serverSettings,
			"postListItem","postBox","",false);
};

var InboxList = function (request,serverSettings) {	
	$.extend(serverSettings,{maxMessages:2});
	
	return new GenericMailList("inbox",request,serverSettings,
			"messageListItem","messageBox", ">> ",true);
};var MailItem = function(item) {
	var canvas = $("<div/>").append(item.text);
	
	if(item.attachment != null) {
		var imageCanvas = $("<div/>");

		var attachCanvas = $("<ul/>");
		
		$.each(item.attachment, function(i, attach) {
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
};
var ShowMore = function (frame,onClick) {
	var element = null;
	
	return {
		remove : function() {
			if(element != null) {
				element.remove();
			}
			
			return this;
		},
		draw : function() {
			this.remove();
			
			element = $("<div/>").attr({
				"class": "showMoreClass"
			}).append("Show More...").click(onClick);
			
			frame.append(element);
			
			return this;
		}
	};
};var TimestampBox = function(timestamp) {
	var itsTime = new Date(timestamp);
	
	return $("<span/>").attr({
		"class": "timestampBox"
	}).append(itsTime.toString(dateFormat));
};

var dateFormat = "dd/MM/yyyy (HH:mm)";var AppContainer = function(id,container) {
		var selected = false;
		var needRefresh = true;
		
		var frame = $("<div/>").attr({
			"id": id+"ApplicationFrame",
			"class": "applicationContainer"
		})	.appendTo(container)
			.hide(0);
		
		eWolf.bind("select."+id,function(event,eventId) {
			if(id == eventId) {	
				if(!selected) {
					frame.show(0);
					frame.animate({
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
					frame.animate({
						opacity : 0,
					}, 300, function() {
						frame.hide(0);
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
				eWolf.trigger("refresh."+id,[id]);
			}
		});
		
		return {
			getFrame : function() {
				return frame;
			},
			getId : function() {
				return id;
			},
			isSelected : function() {
				return selected;
			},
			destroy : function() {
				eWolf.unbind("select."+id);
				frame.remove();
				delete this;
			}
		};
};var Inbox = function (id,applicationFrame) {
	var appContainer = new AppContainer(id,applicationFrame);
	var frame = appContainer.getFrame();
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh();
	
	new TitleArea("Inbox")
		.appendTo(frame)
		.addFunction("New Message...", function() {
			var box = new NewMessageBox(id,applicationFrame);
			box.select();
		});
	
	new InboxList(request,{}).appendTo(frame);
	
	return {
		getId : function() {
			return id;
		},
		isSelected : function() {
			return appContainer.isSelected();
		},
		destroy : function() {
			eWolf.unbind("refresh."+id);
			appContainer.destroy();
			delete this;
		}
	};
};
var NewMessageBox = function(id,applicationFrame,sendToID,sendToName) {
	var obj = this;
	var thisID = "__newmessage__"+id;
	
	var appContainer = new AppContainer(thisID,applicationFrame);
	var frame = appContainer.getFrame();
	
	var request = new PostRequestHandler(thisID,"/json",0);
	
	var topTitle = new TitleArea("Send new message")
		.appendTo(frame)
		.addFunction("Send", send)
		.addFunction("Cancel",cancel);
	
	var base = $("<table/>").appendTo(frame);
	
	var idRaw = $("<tr/>").appendTo(base);
	$("<td/>").attr("style","text-align:right;").append("Send to:").appendTo(idRaw);
	
	var userIdText = $("<input/>").attr({
		"id": "sendToId",
		"type": "text",
		"placeholder": "Send to (user id)",
		"style": "width:300px !important;"
	});
	
	var userIdCell = $("<td/>").append(userIdText).appendTo(idRaw);
	
	var msgRaw = $("<tr/>").appendTo(base);
	$("<td/>").attr("style","vertical-align:text-top;text-align:right;").append("Message:").appendTo(msgRaw);
	
	var messageText = $("<textarea/>").attr({
		"id": "textileMessage",
		"placeholder": "Your message",
		"style": "width:300px !important;height:300px  !important;"
	});
	
	$("<td/>").append(messageText).appendTo(msgRaw);
	
	var attacheRaw = $("<tr/>").appendTo(base);
	$("<td/>").attr("style","vertical-align:text-top;text-align:right;").append("Attachment:").appendTo(attacheRaw);
	
	var uploaderArea = $("<td/>").appendTo(attacheRaw);
	
	/*var files = */new filedrag(uploaderArea);

	var btnRaw = $("<tr/>").appendTo(base);
	
	$("<td/>").appendTo(btnRaw);
	var btnBox = $("<td/>").appendTo(btnRaw);
	
	$("<input/>").attr({
		"id": "sendMessageBtn",
		"type": "button",
		"value": "Send"
	}).appendTo(btnBox).click(send);
	
	btnBox.append("&nbsp;");
	
	$("<input/>").attr({
		"id": "sabortBtn",
		"type": "button",
		"value": "Cancel"
	}).appendTo(btnBox).click(cancel);
	
	var errorRaw = $("<tr/>").appendTo(base);
	
	$("<td/>").appendTo(errorRaw);
	var errorBox = $("<td/>").appendTo(errorRaw);
	
	var errorMessage = $("<span/>").attr({
		"style": "color:red;"
	}).appendTo(errorBox);
	
	function handleResponse(data,postData) {
		if (data.sendMessage != null) {
			if(data.sendMessage.result == "success") {
				obj.destroy();
			} else {
				errorMessage.html(data.sendMessage.result);
			}
		} else {
			console.log("No sendMessage parameter in response");
		}		
	}
	
	function cancel() {
		obj.destroy();
	}
	
	function send() {		
//		var uploader = new qq.FileUploaderBasic({
//		    // path to server-side upload script
//		    action: '/sfsupload',
//		    params: {
//		    	wolfpacks: "someWolfpack"
//		    }
//		});
//		
//		uploader._uploadFileList(files.getFiles());
		
		var msg = messageText.val().replace(/\n/g,"<br>");
		var mailObject = {
				text: msg,
				attachment: [{
					filename: "testfile.doc",
					contentType: "document",
					path: "http://www.google.com"
				},
				{
					filename: "image.jpg",
					contentType: "image/jpeg",
					path: "https://www.cia.gov/library/publications/the-world-factbook/graphics/flags/large/is-lgflag.gif"					
				}]
		};
		
		console.log(JSON.stringify(mailObject));
		
		request.request({
			sendMessage: {
				userID: userIdText.val(),
				message: JSON.stringify(mailObject)
			}
		  },handleResponse);
	}
	
	eWolf.bind("refresh",function(event,eventID) {
		if(eventID == thisID) {
			if(sendToID != null) {
				userIdText.attr("value",sendToID);
				
				if(sendToName != null) {
					userIdText.hide();
					userIdCell.append(new User(sendToID,sendToName));
				}
				
				window.setTimeout(function () {
					messageText.focus();
				}, 0);				
			} else {
				window.setTimeout(function () {
					userIdText.focus();
				}, 0);
			}
		}		
	});
	
	eWolf.bind("select."+id,function(event,eventId) {
		if(eventId != thisID) {
			appContainer.destroy();
			delete obj;
		}
	});
	
	this.select = function() {
		eWolf.trigger("select",[thisID]);
	};
	
	this.destroy = function() {
		eWolf.trigger("select",[id]);
	};

	return this;
};

var Profile = function (id,name,applicationFrame) {
	var obj = this;
	
	var appContainer = new AppContainer(id,applicationFrame);
	var frame = appContainer.getFrame();
	
	var waitingForName = [];
	
	var userObj = {};
	
	if(id != eWolf.data("userID")) {
		userObj.userID = id;
	}
	
	var newsFeedObj = {
			newsOf:"user"
		};
	$.extend(newsFeedObj,userObj);
	
	var handleProfileResonse = new ResonseHandler("profile",
			["id","name"],handleProfileData);
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh()
		.register(getProfileData,handleProfileResonse)
		.register(geWolfpacksData,new ResonseHandler("wolfpacks",
				["wolfpacksList"],handleWolfpacksData));
	
	if(name == null) {
		request.request(getProfileData(),handleProfileResonse);
	}		
	
	var topTitle = new TitleArea(name).appendTo(frame);
	
	if(id != eWolf.data("userID")) {
		topTitle.addFunction("Send message...", function (event) {
			var box = new NewMessageBox(id,applicationFrame,id,name);
			box.select();
		});
	}
	
	var idRow = $("<span/>").attr("class","idBox");
	topTitle.appendAtTitleTextArea("&nbsp;");
	topTitle.appendAtTitleTextArea(idRow);
	
	var wolfpacksContainer = new CommaSeperatedList("Wolfpakcs");
	topTitle.appendAtBottomPart(wolfpacksContainer.getList());
	
	if(id != eWolf.data("userID")) {
		topTitle.addFunction("Add to wolfpack...", function () {
			// TODO: add to any wolfpack (not just wall-readers) 
			request.request({
				addWolfpackMember: {
					wolfpackName: "wall-readers",
					userID: id
				}
			},new ResonseHandler("addWolfpackMember",
					[],function (data, textStatus, postData) {
				request.requestAll();
				eWolf.trigger("needRefresh.__pack__"+postData.addWolfpackMember.wolfpackName);
			}));
		});
	} else {
		topTitle.addFunction("Post", function() {
			// TODO: post on wolfpack
			alert("This will post to a wolfpack");
		});
	}
	
	new NewsFeedList(request,newsFeedObj).appendTo(frame);
	
	var profileData = null;
	var wolfpackData = null;
	
	function handleProfileData(data, textStatus, postData) {
		topTitle.setTitle(new User(data.id,data.name));
		idRow.html(data.id);
		
		name = data.name;
		
		while(waitingForName.length > 0) {
			waitingForName.pop()(name);
		}
	  }
	
	function handleWolfpacksData(data, textStatus, postData) {		
		wolfpacksContainer.removeAll();		 

		 $.each(data.wolfpacksList,function(i,pack) {
			 wolfpacksContainer.addItem(new Wolfpack(pack));
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
	
	this.getID = function() {
		if(profileData != null) {
			return profileData.id;
		} else {
			return id;
		}			
	};
	
	this.getName = function() {
		if(profileData != null) {
			return profileData.name;
		} else {
			return id;
		}				
	};
	
	this.getWolfpacks = function() {
		if(wolfpackData != null) {
			return wolfpackData.wolfpacksList;
		} else {
			return [];
		}			
	};
	
	this.isSelected = function() {
		return appContainer.isSelected();
	};
	
	this.onReceiveName = function(nameHandler) {
		if(name != null) {
			nameHandler(name);
		} else {
			waitingForName.push(nameHandler);
		}
		
		return obj;
	};
	
	this.destroy = function() {
		eWolf.unbind("refresh."+id);
		appContainer.destroy();
		delete obj;
	};
	
	return this;
};
var SearchApp = function(id,menu,applicationFrame,query,searchBtn) {
	var menuList = menu.createNewMenuList("search","Searches");
	var apps = new Object();
	var lastSearch = null;
	
	function addSearchMenuItem(key,name) {
		menuList.addMenuItem(key,name);
		apps[key] = new Profile(key,name,applicationFrame)
			.onReceiveName(function(newName) {
				menuList.renameMenuItem(key,newName);
				eWolf.trigger("select",[key]);
			});	
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
	
	function searchUser(key,name) {
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
	}
	
//	addBtn.click(function() {
//		var key = query.val();
//		if(key != "") {
//			if(key != eWolf.data("userID")) {
//				addSearchMenuItem(key,"Show "+key);
//			}
//			eWolf.trigger("select",[key]);
//		}
//	});
	
	searchBtn.click(function() {
		var key = query.val();
		searchUser(key,"Search: "+key);	
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
//	    	addBtn.hide(200);
	    } else {
	    	searchBtn.show(200);
//	    	addBtn.show(200);
	    }
	});
		
	
	eWolf.bind("search",function(event,key,name) {
		searchUser(key,name);
	});

	
	return {
		search: function (key,name) {
			searchUser(key,name);
		}
	};
};var WolfpackPage = function (id,wolfpackName,applicationFrame) {
	var appContainer = new AppContainer(id,applicationFrame);
	var frame = appContainer.getFrame();
	
	var request = new PostRequestHandler(id,"/json",60)
		.listenToRefresh();
				
	var topTitle = new TitleArea(new Wolfpack(wolfpackName))
		.appendTo(frame)
		.addFunction("Post", function() {
			// TODO: post on wolfpack
			alert("This will post to this wolfpack");
		});
	
	if(wolfpackName != "wall-readers") {
		request.register(getWolfpacksMembersData,
				new ResonseHandler("wolfpackMembers",
						["membersList"],handleWolfpacksMembersData));
		topTitle.addFunction("Add member...", function() {
			// TODO: add member
			alert("This will add a member to a wolfpack");
		});
	} else {
		topTitle.setTitle("News Feed");
	}		
	
	var members = new CommaSeperatedList("Members");
	topTitle.appendAtBottomPart(members.getList());
	
	new NewsFeedList(request,{
		newsOf:"wolfpack",
		wolfpackName:wolfpackName
	}).appendTo(frame);	
	
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
			members.addItem(new User(member.id, member.name));
		});
	}
	
	return {
		getID : function() {
			return id;			
		},
		getName : function() {
			return wolfpackName;			
		},
		isSelected : function() {
			return appContainer.isSelected();
		},
		destroy : function() {
			appContainer.destroy();
			delete this;
		}
	};
};
