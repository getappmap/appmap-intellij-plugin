import {a as a$1}from'./chunk-2AOXOOGQ.js';import {A}from'./chunk-D2SVBYR5.js';import {qa,X,Ya,Ka,Na,rb,Sa,Ra,Xa,i,a,b,eb,ib,O as O$1,P}from'./chunk-M3BAIORW.js';import {j}from'./chunk-FMTW4EUV.js';j();function ce(e,l){return !!e.children(l).length}function de(e){return D(e.v)+":"+D(e.w)+":"+D(e.name)}var te=/:/g;function D(e){return e?String(e).replace(te,"\\:"):""}function Q(e,l){l&&e.attr("style",l);}function pe(e,l,c){l&&e.attr("class",l).attr("class",c+" "+e.attr("class"));}function be(e,l){var c=l.graph();if(qa(c)){var a=c.transition;if(X(a))return a(e)}return e}j();function Y(e,l){var c=e.append("foreignObject").attr("width","100000"),a=c.append("xhtml:div");a.attr("xmlns","http://www.w3.org/1999/xhtml");var i=l.label;switch(typeof i){case "function":a.insert(i);break;case "object":a.insert(function(){return i});break;default:a.html(i);}Q(a,l.labelStyle),a.style("display","inline-block"),a.style("white-space","nowrap");var d=a.node().getBoundingClientRect();return c.attr("width",d.width).attr("height",d.height),c}j();var Z={},le=function(e){let l=Object.keys(e);for(let c of l)Z[c]=e[c];},O=async function(e,l,c,a,i,d){let w=a.select(`[id="${c}"]`),n=Object.keys(e);for(let p of n){let r=e[p],y="default";r.classes.length>0&&(y=r.classes.join(" ")),y=y+" flowchart-label";let h=Ya(r.styles),t=r.text!==void 0?r.text:r.id,s;if(Ka.info("vertex",r,r.labelType),r.labelType==="markdown")Ka.info("vertex",r,r.labelType);else if(Na(rb().flowchart.htmlLabels))s=Y(w,{label:t}).node(),s.parentNode.removeChild(s);else {let k=i.createElementNS("http://www.w3.org/2000/svg","text");k.setAttribute("style",h.labelStyle.replace("color:","fill:"));let $=t.split(Sa.lineBreakRegex);for(let A of $){let S=i.createElementNS("http://www.w3.org/2000/svg","tspan");S.setAttributeNS("http://www.w3.org/XML/1998/namespace","xml:space","preserve"),S.setAttribute("dy","1em"),S.setAttribute("x","1"),S.textContent=A,k.appendChild(S);}s=k;}let b=0,o="";switch(r.type){case "round":b=5,o="rect";break;case "square":o="rect";break;case "diamond":o="question";break;case "hexagon":o="hexagon";break;case "odd":o="rect_left_inv_arrow";break;case "lean_right":o="lean_right";break;case "lean_left":o="lean_left";break;case "trapezoid":o="trapezoid";break;case "inv_trapezoid":o="inv_trapezoid";break;case "odd_right":o="rect_left_inv_arrow";break;case "circle":o="circle";break;case "ellipse":o="ellipse";break;case "stadium":o="stadium";break;case "subroutine":o="subroutine";break;case "cylinder":o="cylinder";break;case "group":o="rect";break;case "doublecircle":o="doublecircle";break;default:o="rect";}let _=await Ra(t,rb());l.setNode(r.id,{labelStyle:h.labelStyle,shape:o,labelText:_,labelType:r.labelType,rx:b,ry:b,class:y,style:h.style,id:r.id,link:r.link,linkTarget:r.linkTarget,tooltip:d.db.getTooltip(r.id)||"",domId:d.db.lookUpDomId(r.id),haveCallback:r.haveCallback,width:r.type==="group"?500:void 0,dir:r.dir,type:r.type,props:r.props,padding:rb().flowchart.padding}),Ka.info("setNode",{labelStyle:h.labelStyle,labelType:r.labelType,shape:o,labelText:_,rx:b,ry:b,class:y,style:h.style,id:r.id,domId:d.db.lookUpDomId(r.id),width:r.type==="group"?500:void 0,type:r.type,dir:r.dir,props:r.props,padding:rb().flowchart.padding});}},ee=async function(e,l,c){Ka.info("abc78 edges = ",e);let a=0,i$1={},d,w;if(e.defaultStyle!==void 0){let n=Ya(e.defaultStyle);d=n.style,w=n.labelStyle;}for(let n of e){a++;let p="L-"+n.start+"-"+n.end;i$1[p]===void 0?(i$1[p]=0,Ka.info("abc78 new entry",p,i$1[p])):(i$1[p]++,Ka.info("abc78 new entry",p,i$1[p]));let r=p+"-"+i$1[p];Ka.info("abc78 new link id to be used is",p,r,i$1[p]);let y="LS-"+n.start,h="LE-"+n.end,t={style:"",labelStyle:""};switch(t.minlen=n.length||1,n.type==="arrow_open"?t.arrowhead="none":t.arrowhead="normal",t.arrowTypeStart="arrow_open",t.arrowTypeEnd="arrow_open",n.type){case "double_arrow_cross":t.arrowTypeStart="arrow_cross";case "arrow_cross":t.arrowTypeEnd="arrow_cross";break;case "double_arrow_point":t.arrowTypeStart="arrow_point";case "arrow_point":t.arrowTypeEnd="arrow_point";break;case "double_arrow_circle":t.arrowTypeStart="arrow_circle";case "arrow_circle":t.arrowTypeEnd="arrow_circle";break}let s="",b="";switch(n.stroke){case "normal":s="fill:none;",d!==void 0&&(s=d),w!==void 0&&(b=w),t.thickness="normal",t.pattern="solid";break;case "dotted":t.thickness="normal",t.pattern="dotted",t.style="fill:none;stroke-width:2px;stroke-dasharray:3;";break;case "thick":t.thickness="thick",t.pattern="solid",t.style="stroke-width: 3.5px;fill:none;";break;case "invisible":t.thickness="invisible",t.pattern="solid",t.style="stroke-width: 0;fill:none;";break}if(n.style!==void 0){let o=Ya(n.style);s=o.style,b=o.labelStyle;}t.style=t.style+=s,t.labelStyle=t.labelStyle+=b,n.interpolate!==void 0?t.curve=Xa(n.interpolate,i):e.defaultInterpolate!==void 0?t.curve=Xa(e.defaultInterpolate,i):t.curve=Xa(Z.curve,i),n.text===void 0?n.style!==void 0&&(t.arrowheadStyle="fill: #333"):(t.arrowheadStyle="fill: #333",t.labelpos="c"),t.labelType=n.labelType,t.label=await Ra(n.text.replace(Sa.lineBreakRegex,`
`),rb()),n.style===void 0&&(t.style=t.style||"stroke: #333; stroke-width: 1.5px;fill:none;"),t.labelStyle=t.labelStyle.replace("color:","fill:"),t.id=r,t.classes="flowchart-link "+y+" "+h,l.setEdge(n.start,n.end,t,a);}},ae=function(e,l){return l.db.getClasses()},oe=async function(e,l,c,a$2){Ka.info("Drawing flowchart");let i=a$2.db.getDirection();i===void 0&&(i="TD");let{securityLevel:d,flowchart:w}=rb(),n=w.nodeSpacing||50,p=w.rankSpacing||50,r;d==="sandbox"&&(r=a("#i"+l));let y=d==="sandbox"?a(r.nodes()[0].contentDocument.body):a("body"),h=d==="sandbox"?r.nodes()[0].contentDocument:document,t=new A({multigraph:true,compound:true}).setGraph({rankdir:i,nodesep:n,ranksep:p,marginx:0,marginy:0}).setDefaultEdgeLabel(function(){return {}}),s,b$1=a$2.db.getSubGraphs();Ka.info("Subgraphs - ",b$1);for(let f=b$1.length-1;f>=0;f--)s=b$1[f],Ka.info("Subgraph - ",s),a$2.db.addVertex(s.id,{text:s.title,type:s.labelType},"group",void 0,s.classes,s.dir);let o=a$2.db.getVertices(),_=a$2.db.getEdges();Ka.info("Edges",_);let k=0;for(k=b$1.length-1;k>=0;k--){s=b$1[k],b("cluster").append("text");for(let f=0;f<s.nodes.length;f++)Ka.info("Setting up subgraphs",s.nodes[f],s.id),t.setParent(s.nodes[f],s.id);}await O(o,t,l,y,h,a$2),await ee(_,t);let $=y.select(`[id="${l}"]`),A$1=y.select("#"+l+" g");if(await a$1(A$1,t,["point","circle","cross"],"flowchart",l),eb.insertTitle($,"flowchartTitleText",w.titleTopMargin,a$2.db.getDiagramTitle()),ib(t,$,w.diagramPadding,w.useMaxWidth),a$2.db.indexNodes("subGraph"+k),!w.htmlLabels){let f=h.querySelectorAll('[id="'+l+'"] .edgeLabel .label');for(let x of f){let m=x.getBBox(),g=h.createElementNS("http://www.w3.org/2000/svg","rect");g.setAttribute("rx",0),g.setAttribute("ry",0),g.setAttribute("width",m.width),g.setAttribute("height",m.height),x.insertBefore(g,x.firstChild);}}Object.keys(o).forEach(function(f){let x=o[f];if(x.link){let m=a("#"+l+' [id="'+f+'"]');if(m){let g=h.createElementNS("http://www.w3.org/2000/svg","a");g.setAttributeNS("http://www.w3.org/2000/svg","class",x.classes.join(" ")),g.setAttributeNS("http://www.w3.org/2000/svg","href",x.link),g.setAttributeNS("http://www.w3.org/2000/svg","rel","noopener"),d==="sandbox"?g.setAttributeNS("http://www.w3.org/2000/svg","target","_top"):x.linkTarget&&g.setAttributeNS("http://www.w3.org/2000/svg","target",x.linkTarget);let z=m.insert(function(){return g},":first-child"),G=m.select(".label-container");G&&z.append(function(){return G.node()});let P=m.select(".label");P&&z.append(function(){return P.node()});}}});},ve={setConf:le,addVertices:O,addEdges:ee,getClasses:ae,draw:oe},ne=(e,l)=>{let c=P,a=c(e,"r"),i=c(e,"g"),d=c(e,"b");return O$1(a,i,d,l)},se=e=>`.label {
    font-family: ${e.fontFamily};
    color: ${e.nodeTextColor||e.textColor};
  }
  .cluster-label text {
    fill: ${e.titleColor};
  }
  .cluster-label span,p {
    color: ${e.titleColor};
  }

  .label text,span,p {
    fill: ${e.nodeTextColor||e.textColor};
    color: ${e.nodeTextColor||e.textColor};
  }

  .node rect,
  .node circle,
  .node ellipse,
  .node polygon,
  .node path {
    fill: ${e.mainBkg};
    stroke: ${e.nodeBorder};
    stroke-width: 1px;
  }
  .flowchart-label text {
    text-anchor: middle;
  }
  // .flowchart-label .text-outer-tspan {
  //   text-anchor: middle;
  // }
  // .flowchart-label .text-inner-tspan {
  //   text-anchor: start;
  // }

  .node .katex path {
    fill: #000;
    stroke: #000;
    stroke-width: 1px;
  }

  .node .label {
    text-align: center;
  }
  .node.clickable {
    cursor: pointer;
  }

  .arrowheadPath {
    fill: ${e.arrowheadColor};
  }

  .edgePath .path {
    stroke: ${e.lineColor};
    stroke-width: 2.0px;
  }

  .flowchart-link {
    stroke: ${e.lineColor};
    fill: none;
  }

  .edgeLabel {
    background-color: ${e.edgeLabelBackground};
    rect {
      opacity: 0.5;
      background-color: ${e.edgeLabelBackground};
      fill: ${e.edgeLabelBackground};
    }
    text-align: center;
  }

  /* For html labels only */
  .labelBkg {
    background-color: ${ne(e.edgeLabelBackground,.5)};
    // background-color: 
  }

  .cluster rect {
    fill: ${e.clusterBkg};
    stroke: ${e.clusterBorder};
    stroke-width: 1px;
  }

  .cluster text {
    fill: ${e.titleColor};
  }

  .cluster span,p {
    color: ${e.titleColor};
  }
  /* .cluster div {
    color: ${e.titleColor};
  } */

  div.mermaidTooltip {
    position: absolute;
    text-align: center;
    max-width: 200px;
    padding: 2px;
    font-family: ${e.fontFamily};
    font-size: 12px;
    background: ${e.tertiaryColor};
    border: 1px solid ${e.border2};
    border-radius: 2px;
    pointer-events: none;
    z-index: 100;
  }

  .flowchartTitleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${e.textColor};
  }
`,Se=se;export{ce as a,de as b,Q as c,pe as d,be as e,Y as f,ve as g,Se as h};//# sourceMappingURL=chunk-H36HDQF7.js.map
//# sourceMappingURL=chunk-H36HDQF7.js.map