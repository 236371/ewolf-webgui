function menuItemClick() {
	if($(this).data('menuItem').selected == false && 
			$(this).data('menuItem').OnRefresh == false) {
		$(this).data('menuItem').select(false);
	}	
}

function refreshClick() {
	if($(this).data('menuItem').OnRefresh == true) {
		$(this).data('menuItem').select(true);
	}	
}

function menuItemOverRefresh() {
	$(this).data('menuItem').OnRefresh = true;
}

function menuItemOutRefresh() {
	$(this).data('menuItem').OnRefresh = false;
}

function MenuItem(key,title) {
	this.key = key;
	
	this.listItem = $("<li/>").attr({
		class: "side frameSelector",
		id: key
	}).appendTo("#menuUL").data('menuItem',this)
				.click(menuItemClick)
				.data('selected',false);
	
	this.aObj = $("<a/>").append(title).appendTo(this.listItem)
					.data('menuItem',this);					
	
	this.refreshAlign = $("<div/>").attr({
		class: "refreshButtonAlign",
		id: key
	}).appendTo(this.aObj).data('menuItem',this);	
	
	this.refresh = $("<input/>").attr({
		id: key,
		type: "button",
		class: "refreshButton",
		value: "R"
	}).appendTo(this.refreshAlign).data('menuItem',this).click(refreshClick);
	
	this.refresh.mouseover(menuItemOverRefresh);
	this.refresh.mouseout(menuItemOutRefresh);
	
	this.frame = $("<div/>").attr({
		id: key+"Frame",
		class: "mainFrameClass"
	}).appendTo("#mainFrame").hide().data('menuItem',this);
	
	this.OnRefresh = false;
	this.selected = false;
}

/*!
 * Just for json tests and some site content
 * Load flicker photos via json
 */
function loadFlicker(tag, place, count,loader) {
	loader.showLoading();
	
	$.getJSON("http://api.flickr.com/services/feeds/photos_public.gne?jsoncallback=?",
			  {
			    tags: tag,
			    tagmode: "any",
			    format: "json"
			  }, function(data) {
				  console.log(data);
			    $.each(data.items, function(i,item){
			    	var c = $("<a/>").attr({
			    		href: item.link,
			    		target: "_TRG"+item.title
			    	}).appendTo(place);
			    	
			    	$("<img/>").attr({
			    		src: item.media.m,
			    		style: "padding:5px 5px 5px 5px; height:130px;"
			    	}).appendTo(c);
			    	
			    	$("<em/>").append("&nbsp;").appendTo(place);
			    	
				    if ( i == count-1 ) {
				      return false;
				    }
			    });
			    
			    loader.hideLoading();
			  });
}

MenuItem.prototype.select = function(refresh) {
	if(refresh) {
		this.frame.html('');
	}
	
	$(".side").removeClass("currentMenuSelection");
	//$(".side").addClass("frameSelector");
	$.each($(".side"),function(i,item) {
		item.data('menuItem').selected = false;
	});
	//$(".refreshButton").removeClass("frameSelector");
	this.listItem.addClass("currentMenuSelection");
	this.selected = true;
	//this.listItem.removeClass("frameSelector");

	$(".mainFrameClass").slideUp(700);		
	
	if(this.frame.is(":empty")) {
		loadFlicker(this.key,this.frame,100,
				new Loading($("#loading"),this.refresh));
	} else {
		//this.refresh.addClass("frameSelector");
	}
	
	this.frame.slideDown(700);
};

function unSelectIt(e) {
	
}

MenuItem.prototype.setSelect = function() {
	this.selected = true;
};

MenuItem.prototype.setUnselect = function() {
	this.selected = false;
};