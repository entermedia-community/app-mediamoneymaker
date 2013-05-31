var transSpeedOffers = 1500;
var waitTimeOffers = 8000;

$(document).ready(function() {
	$('#homepage-offers').cycle({
		fx: 'fade', // name of transition effect (or comma separated names, ex: 'fade,scrollUp,shuffle') List of Effects: jquery.malsup.com/cycle/browser.html
		autostop: false, // true to end slideshow after X transitions (where X == slide count) 
		autostopCount: 0, // number of transitions (optionally used with autostop to define X) 
		speed: transSpeedOffers, // speed of the transition (any valid fx speed value)
		timeout: waitTimeOffers, // milliseconds between slide transitions (0 to disable auto advance)
		sync: true, // true if in-out transitions should occur simultaneously 
		startingSlide: 0,
	});
});
