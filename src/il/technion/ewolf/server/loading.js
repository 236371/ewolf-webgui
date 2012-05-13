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
};

function Loading(_indicator,_activator) {

	this.indicator = _indicator;
	this.activator = _activator;		
	//this.playInterval = 0;
}

Loading.prototype.showLoading = function() {
	this.activator.data("_OldValue_",this.activator.attr("value"));
	this.indicator.spin(spinnerOpts);
	this.activator.attr("value"," ");
	this.indicator.show(200);	

//	clearInterval(this.playInterval);
//	playLoadingInterval = setInterval(function () {
//		indicator.animate({
//		    opacity: 0.1
//		  }, 500);
//		indicator.animate({
//		    opacity: 0.6
//		  }, 500);
//	}, 500);
};

Loading.prototype.hideLoading = function() {
	this.activator.addClass("frameSelector");
	this.activator.attr("value",this.activator.data("_OldValue_"));
	this.indicator.hide(200);
	this.indicator.data('spinner').stop();
//	clearInterval(this.playInterval);
};