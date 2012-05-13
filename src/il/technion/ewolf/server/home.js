var refreshPage = false;

function menuOut() {
	$("#menu").stop();
	
	$("#menu").animate({
	    opacity: 0.25,
	    left: '-175px',
	  }, 200, function() {
	    // Animation complete.
	  });
}

function menuIn() {
	$("#menu").stop();
	
	$("#menu").animate({
	    opacity: 0.7,
	    left: '-35px',
	  }, 200, function() {
	    // Animation complete.
	  });
}

function mainFrameGrow() {
	$("#mainFrame").stop();
	$("#menuBack").stop();

	$("#mainFrame").animate({
	    left: '30px',
	    right: '0'
	  }, 200, function() {
	    // Animation complete.
	  });
	
	$("#menuBack").animate({
	    left: '-175px',
	  }, 200, function() {
	    // Animation complete.
	  });
}

function mainFrameShrink() {
	$("#mainFrame").stop();
	$("#menuBack").stop();
	
	$("#mainFrame").animate({
	    left: '170px',
	    right: '0'
	  }, 200, function() {
	    // Animation complete.
	  });
	
	$("#menuBack").animate({
	    left: '-35px',
	  }, 200, function() {
	    // Animation complete.
	  });
}

/*!
 * Load menu from eWolf server.
 * Will be used to load wolfpacks of the user.
 */
function loadMenu() {
	$.getJSON("/json?callBack=?",
	 {
	 tags: "cat",
	 tagmode: "any",
	 format: "json"
	 },
	  function(data) {		  
		  $.each(data, function(i,item){
			  new MenuItem(item.key,item.title);
		    });  
	  });	
}

/*!
 * Main function
 * Initializations
 */
$(document).ready(onDocumentReady);
function onDocumentReady() {
	loadMenu();

	$("#btnHideMenu").click(function() {
		$("#btnHideMenu").hide();
		menuOut();
		mainFrameGrow();		
		$("#btnShowMenu").show();
	});
	
	$("#btnShowMenu").click(function() {
		$("#btnShowMenu").hide();		
		menuIn();
		mainFrameShrink();		
		$("#btnHideMenu").show();
	});
	
	$("#btnUnPin").click(function() {
		mainFrameGrow();
		$(this).hide();
		$("#btnPin").show();
		$("#btnHideMenu").hide();
		$("body").delegate("#menu","mouseover",function() {
			menuIn();
		});
		
		$("body").delegate("#menu","mouseout",function() {
			menuOut();
		});
	});
	
	$("#btnPin").click(function() {
		mainFrameShrink();
		$(this).hide();
		$("#btnUnPin").show();
		$("#btnHideMenu").show();
		$("body").undelegate("#menu","mouseover");		
		$("body").undelegate("#menu","mouseout");
	});	

	$("#btnLoad").click(function() {
		loadMenu();
	});
	
	$("#btnAdd").click(function() {
		var key = $("#txtSearchBox").val();
		createMenuListItem(key,"Show "+key);
		$("#"+key).click();
	});
	
	$("#btnSearch").click(function() {
		$(".side").removeClass("current");
		$(".mainFrameClass").slideUp(1000);
		$("#searchResult").html("");
			
		loadFlicker($("#txtSearchBox").val(),"#searchResult",100,
				$("#loading"),$(this));
		
		$("#searchResult").slideDown(1000);
	});
	
	$("#txtSearchBox").keyup(function(event){
	    if(event.keyCode == 13 && $(this).val() != ""){
	    	if(event.shiftKey) {
	    		$("#btnAdd").click();
	    	} else {
	    		$("#btnSearch").click();
	    	}
	    }
	    
	    if($(this).val() == "") {
	    	$("#btnSearch").hide(200);
	    	$("#btnAdd").hide(200);
	    } else {
	    	$("#btnSearch").show(200);
	    	$("#btnAdd").show(200);
	    }
	});

	$("body").delegate(".side","mouseover",function() {
		$("<div/>").attr({
			id: "itemMessage",
			class: "messageClass"
		}).text("Click to " + $(this).text()).appendTo("#topbarID");
		$(this).data("menuItem").refresh.show();
	});
	
	$("body").delegate(".side","mouseout",function() {
		$("#itemMessage").remove();
		$(this).data("menuItem").refresh.hide();
	});
	
	$("body").delegate(".side,#button","hover",function() {
			$(this).css('cursor','pointer');
		}, function() {
			$(this).css('cursor','auto');		
	});
	
//	$("body").delegate(".frameSelector","click",function() {
//		$(this).data("menuItem").select(refreshPage);
//	});
	
//	$("body").delegate(".refreshButton","mouseover",function() {
//		refreshPage = true;
//	});
//	
//	$("body").delegate(".refreshButton","mouseout",function() {
//		refreshPage = false;
//	});
}