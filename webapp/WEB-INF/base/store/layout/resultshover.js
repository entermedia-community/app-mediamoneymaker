/*
Simple Image Trail script- By JavaScriptKit.com
Visit http://www.javascriptkit.com for this script and more
This notice must stay intact
*/

var offsetfrommouse=[15,-25]; //image x,y offsets from cursor position in pixels. Enter 0,0 for no offset
var displayduration=0; //duration in seconds image should remain visible. 0 for always.
var currentimageheight = 170;	// maximum image size.
var timerid;
var hasshown = false;

if (document.getElementById || document.all){
	document.write('<div id="trailimageid">');
	document.write('</div>');
}

function gettrailobj(){
if (document.getElementById)
return document.getElementById("trailimageid").style
else if (document.all)
return document.all.trailimagid.style
}

function gettrailobjnostyle(){
if (document.getElementById)
return document.getElementById("trailimageid")
else if (document.all)
return document.all.trailimagid
}


function truebody(){
return (!window.opera && document.compatMode && document.compatMode!="BackCompat")? document.documentElement : document.body
}

function showtrail(inId){
	//alert( "1");
	
	stopTimer();
	if( hasshown == false )
	{
		timerid = setTimeout("showHover()", 1200);
	}
	
		document.onmousemove=followmouse;

		//new Image().src = inPath;

		var showdiv = document.getElementById(inId);

		gettrailobjnostyle().innerHTML = showdiv.innerHTML;

//		if( document.onmousemove == followmouse )
//		{
//			gettrailobj().visibility="visible";
//		}
}

function showHover()
{
	gettrailobj().visibility="visible";
}
function stopTimer()
{
	if( timerid != null )
	{
		clearTimeout( timerid );
		timerid = null;
	}
}

function hidetrail(){
	gettrailobj().visibility="hidden";
	document.onmousemove="";
	gettrailobj().left="-500px";
	stopTimer();
}

function followmouse(e)
{
	var xcoord=offsetfrommouse[0];
	var ycoord=offsetfrommouse[1];

	var docwidth=document.all? truebody().scrollLeft+truebody().clientWidth : pageXOffset+window.innerWidth-15;
	var docheight=document.all? Math.min(truebody().scrollHeight, truebody().clientHeight) : Math.min(document.body.offsetHeight, window.innerHeight);

	//if (document.all){
	//	gettrailobjnostyle().innerHTML = 'A = ' + truebody().scrollHeight + '<br>B = ' + truebody().clientHeight;
	//} else {
	//	gettrailobjnostyle().innerHTML = 'C = ' + document.body.offsetHeight + '<br>D = ' + window.innerHeight;
	//}

	if (typeof e != "undefined"){
		if (docwidth - e.pageX < 300){
			xcoord = e.pageX - xcoord - 286; // Move to the left side of the cursor
		} else {
			xcoord += e.pageX;
		}
		if (docheight - e.pageY < (currentimageheight + 110)){
			ycoord += e.pageY - Math.max(0,(110 + currentimageheight + e.pageY - docheight - truebody().scrollTop));
		} else {
			ycoord += e.pageY;
		}
	} else if (typeof window.event != "undefined"){
		if (docwidth - event.clientX < 300){
			xcoord = event.clientX + truebody().scrollLeft - xcoord - 286; // Move to the left side of the cursor
		} else {
			xcoord += truebody().scrollLeft+event.clientX;
		}
		if (docheight - event.clientY < (currentimageheight + 110)){
			ycoord += event.clientY + truebody().scrollTop - Math.max(0,(110 + currentimageheight + event.clientY - docheight));
		} else {
			ycoord += truebody().scrollTop + event.clientY;
		}
	}

	var docwidth=document.all? truebody().scrollLeft+truebody().clientWidth : pageXOffset+window.innerWidth-15;
	var docheight=document.all? Math.max(truebody().scrollHeight, truebody().clientHeight) : Math.max(document.body.offsetHeight, window.innerHeight);

	gettrailobj().left=xcoord+"px";
	gettrailobj().top=ycoord+"px";

	if( hasshown == true && gettrailobj().visibility !="visible" )
	{
//		if( ( showNow ) 
//		{
			gettrailobj().visibility="visible";
//		}
	}
//	else
	if( gettrailobj().visibility == "visible" )
	{
		hasshown = true;
	}
}