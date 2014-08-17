var localMode = false;
var dataUrl = 'http://localhost:8080/kidney/';

var width = 960;
var height = 500;

var color = d3.scale.category20();

var nodeRadius = 10;


var svg = d3.select("#graphVis").append("svg")
    .attr("width", width)
    .attr("height", height);

var div = d3.select("body").append("div")   
    .attr("class", "tooltip")               
    .style("opacity", 0);

svg.append("svg:defs").selectAll("marker")
    .data(["end"])
    .enter().append("svg:marker")
    .attr("id", String)
    .attr("viewBox", "0 -5 10 10")
    .attr("refX", 10)
    .attr("refY", 0)
    .attr("markerUnits","userSpaceOnUse")
    .attr("markerWidth", 12)
    .attr("markerHeight", 12)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-5L10,0L0,5");



//maps node id to node
var nodeMap = d3.map();
//maps edge id to edge
var edgeMap = d3.map();

var clearGraphData = function(){
    nodeMap = d3.map();
    edgeMap = d3.map();
    solNodeSet = d3.set([]);
    solEdgeSet = d3.set([]);
}

var setGraphAndDraw = function(graph){
    for(i = 0; i < graph.nodes.length; i++){
	nodeMap.set(graph.nodes[i].id,graph.nodes[i]);
    }
    for(i = 0; i < graph.links.length; i++){
	edgeMap.set(graph.links[i].id,graph.links[i]);
	graph.links[i].source = nodeMap.get(graph.links[i].sourceId);
	graph.links[i].target = nodeMap.get(graph.links[i].targetId);
    }
    redrawGraph();
}


var readData = function(jsonData){
    clearGraphData();
    d3.json(jsonData, function(error, graph) {
	if (error) {
	    return console.warn(error);
	}	
	setGraphAndDraw(graph);
    });
}

var readDataNetwork = function(datasetName){
    clearGraphData();
    var request = goog.json.serialize({jsonrpc: '2.0', method: 'LoadData', params: {Dataset: datasetName}, id: 'readDataTest'});
	goog.net.XhrIo.send(dataUrl, function(e) {
	    var xhr = e.target;
	    var obj = xhr.getResponseJson();
	    graph = obj.result;
	    //console.log(graph);
	    setGraphAndDraw(graph);
	}, 'POST', request, {'Content-Type': 'application/json'});
    
}

var getNodesByType = function(allNodesMap, nodeType){
    nodeList = [];
    allNodesMap.forEach(function(nodeId, nodeValue){
	if(nodeValue.type === nodeType){
	    nodeList.push(nodeValue);
	}
    });
    return nodeList;
}

var dataSelector =  goog.dom.getElement("datasetSelector");
var addDataset = function(displayName,value){
    option = goog.dom.createElement('option');
    option.innerHTML = displayName;
    option.value = value;
    goog.dom.appendChild(dataSelector,option);
}

if(localMode){
    addDataset("Graph 1",1);
    addDataset("Graph 2",2);
}
else{
    var request = goog.json.serialize({jsonrpc: '2.0', method: 'Datasets', params: {}, id: 'DatasetsTest'});
	goog.net.XhrIo.send(dataUrl, function(e) {
	    var xhr = e.target;
	    var obj = xhr.getResponseJson();
	    listOfDatasets = obj.result;
	    //console.log(listOfDatasets);
	    for(i =0; i < listOfDatasets.length; i++){
		addDataset(listOfDatasets[i],listOfDatasets[i]);
	    }
	}, 'POST', request, {'Content-Type': 'application/json'});
}


goog.events.listen(dataSelector,goog.events.EventType.CLICK,
		   function(e){
		       //console.log(e.target.value);
		       if(localMode){
			   readData("data/graph"+e.target.value +".json");
		       }
		       else{
			   readDataNetwork(e.target.value);
		       }
		   });

var key = function(e){
    return e.id;
}

var solNodeSet = d3.set([]);
var solEdgeSet = d3.set([]);

var button = goog.dom.getElement('solveButton');
goog.events.listen(
    button,
    goog.events.EventType.CLICK,
    function(e){
	if(localMode){
	    updateSolution("data/solution"+dataSelector.value+".json");
	}
	else{
	    updateSolutionNetwork(dataSelector.value);
	}
    }
);

var updateSolution = function(solutionJsonFile){    
    d3.json(solutionJsonFile, function(error, solution) {
	if (error) {
	    return console.warn(error);
	}
	updateSolutionWithEdgeSet(solution);
	//console.log(solEdgeSet);	
    });
}

var updateSolutionNetwork = function(datasetName){
    var request = goog.json.serialize({jsonrpc: '2.0', method: 'Solve', params: {Dataset: datasetName}, id: 'solutionTest'});
	goog.net.XhrIo.send(dataUrl, function(e) {
	    var xhr = e.target;
	    var obj = xhr.getResponseJson();
	    solutionJson = obj.result;
	    console.log(solutionJson);
	    updateSolutionWithEdgeSet(solutionJson);
	}, 'POST', request, {'Content-Type': 'application/json'});    
}

var updateSolutionWithEdgeSet = function(solution){
    solNodeSet = d3.set([]);
    solEdgeSet = d3.set([]);
    for(i = 0; i < solution.links.length; i++){
	solEdgeSet.add(solution.links[i].id);
	solNodeSet.add(solution.links[i].sourceId);
	solNodeSet.add(solution.links[i].targetId);
    }
    redrawGraph();
}

var hideUnreachable = false;
var checkUnreachable = goog.dom.getElement('checkUnreachable');
goog.events.listen(checkUnreachable,goog.events.EventType.CLICK,function(e){
    hideUnreachable = e.target.checked;
    redrawGraph();
});

var hideUnmatchedChips = false;
var checkUnmatchedChips = goog.dom.getElement('checkUnmatchedChips');
goog.events.listen(checkUnmatchedChips,goog.events.EventType.CLICK,function(e){
    hideUnmatchedChips = e.target.checked;
    redrawGraph();
});

var hideNotInSolution = false;
var checkNotInSolution = goog.dom.getElement('checkNotInSolution');
goog.events.listen(checkNotInSolution,goog.events.EventType.CLICK,function(e){
    hideNotInSolution = e.target.checked;
    redrawGraph();
});

//returns true iff we should keep the node
var nodeFilter = function(node){
    if(hideUnreachable && (!node.reachable)){
	return false;
    }
    if(hideNotInSolution && (!solNodeSet.has(node.id))){
	return false;
    }
    if(hideUnmatchedChips && (!solNodeSet.has(node.id)) && node.type === "terminal"){
	return false;
    }
    return true;
}

var edgeFilter = function(edge){
    if(hideNotInSolution && !solEdgeSet.has(edge.id)){
	return false;
    }
    if(!nodeFilter(nodeMap.get(edge.sourceId))){
	return false;
    }
    if(!nodeFilter(nodeMap.get(edge.targetId))){
	return false;
    }
    return true;
}

var makeDisplayedSubgraph = function(){
    var subgraph = {nodeById:d3.map(), edgeById:d3.map()};    
    nodeMap.forEach(function(nodeId, node){
	if(nodeFilter(node)){
	    subgraph.nodeById.set(nodeId,node);
	}
    });
    edgeMap.forEach(function(edgeId, edge){
	if(edgeFilter(edge)){
	    subgraph.edgeById.set(edgeId,edge);
	}
    });
    return subgraph;    
}


var redrawGraph = function(){
    var displayedGraph = makeDisplayedSubgraph();
    //console.log(displayedGraph.nodeById.values());
    //console.log(displayedGraph.edgeById.values());
    var force = d3.layout.force()
	.charge(-120)
	.linkDistance(90)
	.size([width, height])
	.nodes(displayedGraph.nodeById.values())
	.links(displayedGraph.edgeById.values())
	.start();
    
    

    //console.log("sol edge set: " + solEdgeSet.values());
    //console.log("sol node set: " + solNodeSet.values());
    var link = svg.selectAll(".link")
	.data(displayedGraph.edgeById.values(),key);
    //enter
    link
	.enter()
	.append("path")
	.attr("id", function(d){return d.id})
	.attr("class", "link")
	.attr("stroke","black")	
	.attr("fill","none")
	.attr("marker-end", "url(#end)");
    //update
    link.attr("stroke-width",function(d){
	    //console.log(d.id);
	    return solEdgeSet.has(d.id) ? "4" : "2";
    });
    //exit!
    link.exit().remove();
    
    
    nodeTypes = ["root","paired","terminal"];
    for(i = 0; i < nodeTypes.length; i++){
	nodeClass = "." + nodeTypes[i];
	var node = svg.selectAll(nodeClass)	
	    .data(getNodesByType(displayedGraph.nodeById,nodeTypes[i]),key);
	var nodeEnter = node.enter()
	    .append("g")
	    .attr("class", nodeTypes[i])
	    .call(force.drag)
	    /*.append("title")
	    .text(function(d) { return d.id; })	    
	    .append("text")
	    .attr("x",0)
	    .attr("y",0)	    
	    .attr("fill","black")
	    .text(function(d) { return d.id })*/
	    .on("mouseover", function(d) {      
		div.transition()        
                    .duration(200)      
                    .style("opacity", .9);      
		div.html(d.id)  
                    .style("left", (d3.event.pageX) + "px")     
                    .style("top", (d3.event.pageY - 28) + "px");    
            })                  
            .on("mouseout", function(d) {       
		div.transition()        
                    .duration(500)      
                    .style("opacity", 0);   
            });
	if(nodeTypes[i] === "root"){
	    //console.log(i);
	    nodeEnter.append("polygon")
		.attr("points","0,20 20,20 10,0")
		//.style("fill-opacity","0.3")
		.style("fill", function(d,i){return color(d.sensitized);});	    
	}
	else if(nodeTypes[i] === "paired"){
	    //console.log(i);
	    nodeEnter.append("circle")
		.attr("r", nodeRadius)
		.attr("cx",0)
		.attr("cy",0)
		//.style("fill-opacity","0.3")
		.style("fill", function(d,i){return color(d.sensitized);});
	}
	else if(nodeTypes[i] === "terminal"){
	    //console.log(i);
	    nodeEnter.append("rect")	
		.attr("x", -nodeRadius)
		.attr("y", -nodeRadius)
		.attr("width", 2*nodeRadius)
		.attr("height", 2*nodeRadius)
		//.style("fill-opacity","0.3")
		.style("fill", function(d,i){return color(d.sensitized);});
	}
	node.exit().remove();
    }
    
    force.on("tick", function(){
	//gives curved lines
	link.attr("d", function(d) {
	    var dx = effectiveX(d.target) - effectiveX(d.source);
	    var dy = effectiveY(d.target) - effectiveY(d.source);
	    var dz = Math.sqrt(dx * dx + dy * dy);
	    var dz2 = Math.max(1,dz - nodeRadius);
	    var scaleDown = dz === 0 ? 0 : dz2/dz;
	    var dx2 = dx*scaleDown;
	    var dy2 = dy*scaleDown;
	    var dr = Math.sqrt(dx2 * dx2 + dy2 * dy2);
	    return "M" + effectiveX(d.source) + "," + effectiveY(d.source) + "A" +
		dr + "," + dr + " 0 0,1 " + (effectiveX(d.source) + dx2)
		+ "," + (effectiveY(d.source) + dy2);
	});
	//gives straight lines
        /*link.attr("x1", function(d) { return d.source.x; })
	  .attr("y1", function(d) { return d.source.y; })
	  .attr("x2", function(d) { return d.target.x; })
	  .attr("y2", function(d) { return d.target.y; });*/

	
	for(i = 0; i < nodeTypes.length; i++){
	    nodeClass = "." + nodeTypes[i];
	    var node = svg.selectAll(nodeClass)
		.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });	    
	}
	
	//back when there was only one kind of node...
	//node.attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; });	    
    });    
}

var effectiveX = function(node){
    if(node.type === "root"){
	return node.x + nodeRadius;
    }
    else{
	return node.x;
    }
}

var effectiveY = function(node){
    if(node.type === "root"){
	return node.y + nodeRadius;
    }
    else{
	return node.y;
    }
}


//readData("data/graph1.json");
