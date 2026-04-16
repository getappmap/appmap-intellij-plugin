import {o,n,nb,mb,pb,ob,lb,kb,jb,rb,Ka,a,L,q,p,hb,w,d,s,K as K$1,J,C,I,H,G,F,E,D,B,A,z as z$1,y,x,r,Sa,eb}from'./chunk-M3BAIORW.js';import {c,j,e}from'./chunk-FMTW4EUV.js';var ve=c((At,Lt)=>{j();(function(t,i){typeof At=="object"&&typeof Lt<"u"?Lt.exports=i():typeof define=="function"&&define.amd?define(i):(t=typeof globalThis<"u"?globalThis:t||self).dayjs_plugin_isoWeek=i();})(At,(function(){var t="day";return function(i,r,a){var s=function(_){return _.add(4-_.isoWeekday(),t)},f=r.prototype;f.isoWeekYear=function(){return s(this).year()},f.isoWeek=function(_){if(!this.$utils().u(_))return this.add(7*(_-this.isoWeek()),t);var x,A,C,F,H=s(this),P=(x=this.isoWeekYear(),A=this.$u,C=(A?a.utc:a)().year(x).startOf("year"),F=4-C.isoWeekday(),C.isoWeekday()>4&&(F+=7),C.add(F,t));return H.diff(P,"week")+1},f.isoWeekday=function(_){return this.$utils().u(_)?this.day()||7:this.day(this.day()%7?_:_-7)};var h=f.startOf;f.startOf=function(_,x){var A=this.$utils(),C=!!A.u(x)||x;return A.p(_)==="isoweek"?C?this.date(this.date()-(this.isoWeekday()-1)).startOf("day"):this.date(this.date()-1-(this.isoWeekday()-1)+7).endOf("day"):h.bind(this)(_,x)};}}));});var xe=c((Ft,Wt)=>{j();(function(t,i){typeof Ft=="object"&&typeof Wt<"u"?Wt.exports=i():typeof define=="function"&&define.amd?define(i):(t=typeof globalThis<"u"?globalThis:t||self).dayjs_plugin_customParseFormat=i();})(Ft,(function(){var t={LTS:"h:mm:ss A",LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D, YYYY",LLL:"MMMM D, YYYY h:mm A",LLLL:"dddd, MMMM D, YYYY h:mm A"},i=/(\[[^[]*\])|([-_:/.,()\s]+)|(A|a|Q|YYYY|YY?|ww?|MM?M?M?|Do|DD?|hh?|HH?|mm?|ss?|S{1,3}|z|ZZ?)/g,r=/\d/,a=/\d\d/,s=/\d\d?/,f=/\d*[^-_:/,()\s\d]+/,h={},_=function(k){return (k=+k)+(k>68?1900:2e3)},x=function(k){return function(m){this[k]=+m;}},A=[/[+-]\d\d:?(\d\d)?|Z/,function(k){(this.zone||(this.zone={})).offset=(function(m){if(!m||m==="Z")return 0;var M=m.match(/([+-]|\d\d)/g),L=60*M[1]+(+M[2]||0);return L===0?0:M[0]==="+"?-L:L})(k);}],C=function(k){var m=h[k];return m&&(m.indexOf?m:m.s.concat(m.f))},F=function(k,m){var M,L=h.meridiem;if(L){for(var N=1;N<=24;N+=1)if(k.indexOf(L(N,0,m))>-1){M=N>12;break}}else M=k===(m?"pm":"PM");return M},H={A:[f,function(k){this.afternoon=F(k,false);}],a:[f,function(k){this.afternoon=F(k,true);}],Q:[r,function(k){this.month=3*(k-1)+1;}],S:[r,function(k){this.milliseconds=100*+k;}],SS:[a,function(k){this.milliseconds=10*+k;}],SSS:[/\d{3}/,function(k){this.milliseconds=+k;}],s:[s,x("seconds")],ss:[s,x("seconds")],m:[s,x("minutes")],mm:[s,x("minutes")],H:[s,x("hours")],h:[s,x("hours")],HH:[s,x("hours")],hh:[s,x("hours")],D:[s,x("day")],DD:[a,x("day")],Do:[f,function(k){var m=h.ordinal,M=k.match(/\d+/);if(this.day=M[0],m)for(var L=1;L<=31;L+=1)m(L).replace(/\[|\]/g,"")===k&&(this.day=L);}],w:[s,x("week")],ww:[a,x("week")],M:[s,x("month")],MM:[a,x("month")],MMM:[f,function(k){var m=C("months"),M=(C("monthsShort")||m.map((function(L){return L.slice(0,3)}))).indexOf(k)+1;if(M<1)throw new Error;this.month=M%12||M;}],MMMM:[f,function(k){var m=C("months").indexOf(k)+1;if(m<1)throw new Error;this.month=m%12||m;}],Y:[/[+-]?\d+/,x("year")],YY:[a,function(k){this.year=_(k);}],YYYY:[/\d{4}/,x("year")],Z:A,ZZ:A};function P(k){var m,M;m=k,M=h&&h.formats;for(var L=(k=m.replace(/(\[[^\]]+])|(LTS?|l{1,4}|L{1,4})/g,(function(g,v,b){var y=b&&b.toUpperCase();return v||M[b]||t[b]||M[y].replace(/(\[[^\]]+])|(MMMM|MM|DD|dddd)/g,(function(n,u,d){return u||d.slice(1)}))}))).match(i),N=L.length,R=0;R<N;R+=1){var U=L[R],X=H[U],B=X&&X[0],j=X&&X[1];L[R]=j?{regex:B,parser:j}:U.replace(/^\[|\]$/g,"");}return function(g){for(var v={},b=0,y=0;b<N;b+=1){var n=L[b];if(typeof n=="string")y+=n.length;else {var u=n.regex,d=n.parser,o=g.slice(y),p=u.exec(o)[0];d.call(v,p),g=g.replace(p,"");}}return (function(e){var Y=e.afternoon;if(Y!==void 0){var c=e.hours;Y?c<12&&(e.hours+=12):c===12&&(e.hours=0),delete e.afternoon;}})(v),v}}return function(k,m,M){M.p.customParseFormat=true,k&&k.parseTwoDigitYear&&(_=k.parseTwoDigitYear);var L=m.prototype,N=L.parse;L.parse=function(R){var U=R.date,X=R.utc,B=R.args;this.$u=X;var j=B[1];if(typeof j=="string"){var g=B[2]===true,v=B[3]===true,b=g||v,y=B[2];v&&(y=B[2]),h=this.$locale(),!g&&y&&(h=M.Ls[y]),this.$d=(function(o,p,e,Y){try{if(["x","X"].indexOf(p)>-1)return new Date((p==="X"?1e3:1)*o);var c=P(p)(o),l=c.year,T=c.month,I=c.day,D=c.hours,S=c.minutes,w=c.seconds,E=c.milliseconds,$=c.zone,tt=c.week,ct=new Date,lt=I||(l||T?1:ct.getDate()),O=l||ct.getFullYear(),q=0;l&&!T||(q=T>0?T-1:ct.getMonth());var V,st=D||0,Z=S||0,et=w||0,G=E||0;return $?new Date(Date.UTC(O,q,lt,st,Z,et,G+60*$.offset*1e3)):e?new Date(Date.UTC(O,q,lt,st,Z,et,G)):(V=new Date(O,q,lt,st,Z,et,G),tt&&(V=Y(V).week(tt).toDate()),V)}catch{return new Date("")}})(U,j,X,M),this.init(),y&&y!==true&&(this.$L=this.locale(y).$L),b&&U!=this.format(j)&&(this.$d=new Date("")),h={};}else if(j instanceof Array)for(var n=j.length,u=1;u<=n;u+=1){B[1]=j[u-1];var d=M.apply(this,B);if(d.isValid()){this.$d=d.$d,this.$L=d.$L,this.init();break}u===n&&(this.$d=new Date(""));}else N.call(this,R);};}}));});var we=c((Ot,Vt)=>{j();(function(t,i){typeof Ot=="object"&&typeof Vt<"u"?Vt.exports=i():typeof define=="function"&&define.amd?define(i):(t=typeof globalThis<"u"?globalThis:t||self).dayjs_plugin_advancedFormat=i();})(Ot,(function(){return function(t,i){var r=i.prototype,a=r.format;r.format=function(s){var f=this,h=this.$locale();if(!this.isValid())return a.bind(this)(s);var _=this.$utils(),x=(s||"YYYY-MM-DDTHH:mm:ssZ").replace(/\[([^\]]+)]|Q|wo|ww|w|WW|W|zzz|z|gggg|GGGG|Do|X|x|k{1,2}|S/g,(function(A){switch(A){case "Q":return Math.ceil((f.$M+1)/3);case "Do":return h.ordinal(f.$D);case "gggg":return f.weekYear();case "GGGG":return f.isoWeekYear();case "wo":return h.ordinal(f.week(),"W");case "w":case "ww":return _.s(f.week(),A==="w"?1:2,"0");case "W":case "WW":return _.s(f.isoWeek(),A==="W"?1:2,"0");case "k":case "kk":return _.s(String(f.$H===0?24:f.$H),A==="k"?1:2,"0");case "X":return Math.floor(f.$d.getTime()/1e3);case "x":return f.$d.getTime();case "z":return "["+f.offsetName()+"]";case "zzz":return "["+f.offsetName("long")+"]";default:return A}}));return a.bind(this)(x)};}}));});j();var Ce=e(o(),1),z=e(n(),1),Se=e(ve(),1),Ee=e(xe(),1),Me=e(we(),1);var Pt=(function(){var t=function(y,n,u,d){for(u=u||{},d=y.length;d--;u[y[d]]=n);return u},i=[6,8,10,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,30,32,33,35,37],r=[1,25],a=[1,26],s=[1,27],f=[1,28],h=[1,29],_=[1,30],x=[1,31],A=[1,9],C=[1,10],F=[1,11],H=[1,12],P=[1,13],k=[1,14],m=[1,15],M=[1,16],L=[1,18],N=[1,19],R=[1,20],U=[1,21],X=[1,22],B=[1,24],j=[1,32],g={trace:function(){},yy:{},symbols_:{error:2,start:3,gantt:4,document:5,EOF:6,line:7,SPACE:8,statement:9,NL:10,weekday:11,weekday_monday:12,weekday_tuesday:13,weekday_wednesday:14,weekday_thursday:15,weekday_friday:16,weekday_saturday:17,weekday_sunday:18,dateFormat:19,inclusiveEndDates:20,topAxis:21,axisFormat:22,tickInterval:23,excludes:24,includes:25,todayMarker:26,title:27,acc_title:28,acc_title_value:29,acc_descr:30,acc_descr_value:31,acc_descr_multiline_value:32,section:33,clickStatement:34,taskTxt:35,taskData:36,click:37,callbackname:38,callbackargs:39,href:40,clickStatementDebug:41,$accept:0,$end:1},terminals_:{2:"error",4:"gantt",6:"EOF",8:"SPACE",10:"NL",12:"weekday_monday",13:"weekday_tuesday",14:"weekday_wednesday",15:"weekday_thursday",16:"weekday_friday",17:"weekday_saturday",18:"weekday_sunday",19:"dateFormat",20:"inclusiveEndDates",21:"topAxis",22:"axisFormat",23:"tickInterval",24:"excludes",25:"includes",26:"todayMarker",27:"title",28:"acc_title",29:"acc_title_value",30:"acc_descr",31:"acc_descr_value",32:"acc_descr_multiline_value",33:"section",35:"taskTxt",36:"taskData",37:"click",38:"callbackname",39:"callbackargs",40:"href"},productions_:[0,[3,3],[5,0],[5,2],[7,2],[7,1],[7,1],[7,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[11,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,1],[9,2],[9,2],[9,1],[9,1],[9,1],[9,2],[34,2],[34,3],[34,3],[34,4],[34,3],[34,4],[34,2],[41,2],[41,3],[41,3],[41,4],[41,3],[41,4],[41,2]],performAction:function(n,u,d,o,p,e,Y){var c=e.length-1;switch(p){case 1:return e[c-1];case 2:this.$=[];break;case 3:e[c-1].push(e[c]),this.$=e[c-1];break;case 4:case 5:this.$=e[c];break;case 6:case 7:this.$=[];break;case 8:o.setWeekday("monday");break;case 9:o.setWeekday("tuesday");break;case 10:o.setWeekday("wednesday");break;case 11:o.setWeekday("thursday");break;case 12:o.setWeekday("friday");break;case 13:o.setWeekday("saturday");break;case 14:o.setWeekday("sunday");break;case 15:o.setDateFormat(e[c].substr(11)),this.$=e[c].substr(11);break;case 16:o.enableInclusiveEndDates(),this.$=e[c].substr(18);break;case 17:o.TopAxis(),this.$=e[c].substr(8);break;case 18:o.setAxisFormat(e[c].substr(11)),this.$=e[c].substr(11);break;case 19:o.setTickInterval(e[c].substr(13)),this.$=e[c].substr(13);break;case 20:o.setExcludes(e[c].substr(9)),this.$=e[c].substr(9);break;case 21:o.setIncludes(e[c].substr(9)),this.$=e[c].substr(9);break;case 22:o.setTodayMarker(e[c].substr(12)),this.$=e[c].substr(12);break;case 24:o.setDiagramTitle(e[c].substr(6)),this.$=e[c].substr(6);break;case 25:this.$=e[c].trim(),o.setAccTitle(this.$);break;case 26:case 27:this.$=e[c].trim(),o.setAccDescription(this.$);break;case 28:o.addSection(e[c].substr(8)),this.$=e[c].substr(8);break;case 30:o.addTask(e[c-1],e[c]),this.$="task";break;case 31:this.$=e[c-1],o.setClickEvent(e[c-1],e[c],null);break;case 32:this.$=e[c-2],o.setClickEvent(e[c-2],e[c-1],e[c]);break;case 33:this.$=e[c-2],o.setClickEvent(e[c-2],e[c-1],null),o.setLink(e[c-2],e[c]);break;case 34:this.$=e[c-3],o.setClickEvent(e[c-3],e[c-2],e[c-1]),o.setLink(e[c-3],e[c]);break;case 35:this.$=e[c-2],o.setClickEvent(e[c-2],e[c],null),o.setLink(e[c-2],e[c-1]);break;case 36:this.$=e[c-3],o.setClickEvent(e[c-3],e[c-1],e[c]),o.setLink(e[c-3],e[c-2]);break;case 37:this.$=e[c-1],o.setLink(e[c-1],e[c]);break;case 38:case 44:this.$=e[c-1]+" "+e[c];break;case 39:case 40:case 42:this.$=e[c-2]+" "+e[c-1]+" "+e[c];break;case 41:case 43:this.$=e[c-3]+" "+e[c-2]+" "+e[c-1]+" "+e[c];break}},table:[{3:1,4:[1,2]},{1:[3]},t(i,[2,2],{5:3}),{6:[1,4],7:5,8:[1,6],9:7,10:[1,8],11:17,12:r,13:a,14:s,15:f,16:h,17:_,18:x,19:A,20:C,21:F,22:H,23:P,24:k,25:m,26:M,27:L,28:N,30:R,32:U,33:X,34:23,35:B,37:j},t(i,[2,7],{1:[2,1]}),t(i,[2,3]),{9:33,11:17,12:r,13:a,14:s,15:f,16:h,17:_,18:x,19:A,20:C,21:F,22:H,23:P,24:k,25:m,26:M,27:L,28:N,30:R,32:U,33:X,34:23,35:B,37:j},t(i,[2,5]),t(i,[2,6]),t(i,[2,15]),t(i,[2,16]),t(i,[2,17]),t(i,[2,18]),t(i,[2,19]),t(i,[2,20]),t(i,[2,21]),t(i,[2,22]),t(i,[2,23]),t(i,[2,24]),{29:[1,34]},{31:[1,35]},t(i,[2,27]),t(i,[2,28]),t(i,[2,29]),{36:[1,36]},t(i,[2,8]),t(i,[2,9]),t(i,[2,10]),t(i,[2,11]),t(i,[2,12]),t(i,[2,13]),t(i,[2,14]),{38:[1,37],40:[1,38]},t(i,[2,4]),t(i,[2,25]),t(i,[2,26]),t(i,[2,30]),t(i,[2,31],{39:[1,39],40:[1,40]}),t(i,[2,37],{38:[1,41]}),t(i,[2,32],{40:[1,42]}),t(i,[2,33]),t(i,[2,35],{39:[1,43]}),t(i,[2,34]),t(i,[2,36])],defaultActions:{},parseError:function(n,u){if(u.recoverable)this.trace(n);else {var d=new Error(n);throw d.hash=u,d}},parse:function(n){var u=this,d=[0],o=[],p=[null],e=[],Y=this.table,c="",l=0,T=0,I=2,D=1,S=e.slice.call(arguments,1),w=Object.create(this.lexer),E={yy:{}};for(var $ in this.yy)Object.prototype.hasOwnProperty.call(this.yy,$)&&(E.yy[$]=this.yy[$]);w.setInput(n,E.yy),E.yy.lexer=w,E.yy.parser=this,typeof w.yylloc>"u"&&(w.yylloc={});var tt=w.yylloc;e.push(tt);var ct=w.options&&w.options.ranges;typeof E.yy.parseError=="function"?this.parseError=E.yy.parseError:this.parseError=Object.getPrototypeOf(this).parseError;function lt(){var J;return J=o.pop()||w.lex()||D,typeof J!="number"&&(J instanceof Array&&(o=J,J=o.pop()),J=u.symbols_[J]||J),J}for(var O,q,V,st,Z={},et,G,bt,yt;;){if(q=d[d.length-1],this.defaultActions[q]?V=this.defaultActions[q]:((O===null||typeof O>"u")&&(O=lt()),V=Y[q]&&Y[q][O]),typeof V>"u"||!V.length||!V[0]){var vt="";yt=[];for(et in Y[q])this.terminals_[et]&&et>I&&yt.push("'"+this.terminals_[et]+"'");w.showPosition?vt="Parse error on line "+(l+1)+`:
`+w.showPosition()+`
Expecting `+yt.join(", ")+", got '"+(this.terminals_[O]||O)+"'":vt="Parse error on line "+(l+1)+": Unexpected "+(O==D?"end of input":"'"+(this.terminals_[O]||O)+"'"),this.parseError(vt,{text:w.match,token:this.terminals_[O]||O,line:w.yylineno,loc:tt,expected:yt});}if(V[0]instanceof Array&&V.length>1)throw new Error("Parse Error: multiple actions possible at state: "+q+", token: "+O);switch(V[0]){case 1:d.push(O),p.push(w.yytext),e.push(w.yylloc),d.push(V[1]),O=null,T=w.yyleng,c=w.yytext,l=w.yylineno,tt=w.yylloc;break;case 2:if(G=this.productions_[V[1]][1],Z.$=p[p.length-G],Z._$={first_line:e[e.length-(G||1)].first_line,last_line:e[e.length-1].last_line,first_column:e[e.length-(G||1)].first_column,last_column:e[e.length-1].last_column},ct&&(Z._$.range=[e[e.length-(G||1)].range[0],e[e.length-1].range[1]]),st=this.performAction.apply(Z,[c,T,l,E.yy,V[1],p,e].concat(S)),typeof st<"u")return st;G&&(d=d.slice(0,-1*G*2),p=p.slice(0,-1*G),e=e.slice(0,-1*G)),d.push(this.productions_[V[1]][0]),p.push(Z.$),e.push(Z._$),bt=Y[d[d.length-2]][d[d.length-1]],d.push(bt);break;case 3:return  true}}return  true}},v=(function(){var y={EOF:1,parseError:function(u,d){if(this.yy.parser)this.yy.parser.parseError(u,d);else throw new Error(u)},setInput:function(n,u){return this.yy=u||this.yy||{},this._input=n,this._more=this._backtrack=this.done=false,this.yylineno=this.yyleng=0,this.yytext=this.matched=this.match="",this.conditionStack=["INITIAL"],this.yylloc={first_line:1,first_column:0,last_line:1,last_column:0},this.options.ranges&&(this.yylloc.range=[0,0]),this.offset=0,this},input:function(){var n=this._input[0];this.yytext+=n,this.yyleng++,this.offset++,this.match+=n,this.matched+=n;var u=n.match(/(?:\r\n?|\n).*/g);return u?(this.yylineno++,this.yylloc.last_line++):this.yylloc.last_column++,this.options.ranges&&this.yylloc.range[1]++,this._input=this._input.slice(1),n},unput:function(n){var u=n.length,d=n.split(/(?:\r\n?|\n)/g);this._input=n+this._input,this.yytext=this.yytext.substr(0,this.yytext.length-u),this.offset-=u;var o=this.match.split(/(?:\r\n?|\n)/g);this.match=this.match.substr(0,this.match.length-1),this.matched=this.matched.substr(0,this.matched.length-1),d.length-1&&(this.yylineno-=d.length-1);var p=this.yylloc.range;return this.yylloc={first_line:this.yylloc.first_line,last_line:this.yylineno+1,first_column:this.yylloc.first_column,last_column:d?(d.length===o.length?this.yylloc.first_column:0)+o[o.length-d.length].length-d[0].length:this.yylloc.first_column-u},this.options.ranges&&(this.yylloc.range=[p[0],p[0]+this.yyleng-u]),this.yyleng=this.yytext.length,this},more:function(){return this._more=true,this},reject:function(){if(this.options.backtrack_lexer)this._backtrack=true;else return this.parseError("Lexical error on line "+(this.yylineno+1)+`. You can only invoke reject() in the lexer when the lexer is of the backtracking persuasion (options.backtrack_lexer = true).
`+this.showPosition(),{text:"",token:null,line:this.yylineno});return this},less:function(n){this.unput(this.match.slice(n));},pastInput:function(){var n=this.matched.substr(0,this.matched.length-this.match.length);return (n.length>20?"...":"")+n.substr(-20).replace(/\n/g,"")},upcomingInput:function(){var n=this.match;return n.length<20&&(n+=this._input.substr(0,20-n.length)),(n.substr(0,20)+(n.length>20?"...":"")).replace(/\n/g,"")},showPosition:function(){var n=this.pastInput(),u=new Array(n.length+1).join("-");return n+this.upcomingInput()+`
`+u+"^"},test_match:function(n,u){var d,o,p;if(this.options.backtrack_lexer&&(p={yylineno:this.yylineno,yylloc:{first_line:this.yylloc.first_line,last_line:this.last_line,first_column:this.yylloc.first_column,last_column:this.yylloc.last_column},yytext:this.yytext,match:this.match,matches:this.matches,matched:this.matched,yyleng:this.yyleng,offset:this.offset,_more:this._more,_input:this._input,yy:this.yy,conditionStack:this.conditionStack.slice(0),done:this.done},this.options.ranges&&(p.yylloc.range=this.yylloc.range.slice(0))),o=n[0].match(/(?:\r\n?|\n).*/g),o&&(this.yylineno+=o.length),this.yylloc={first_line:this.yylloc.last_line,last_line:this.yylineno+1,first_column:this.yylloc.last_column,last_column:o?o[o.length-1].length-o[o.length-1].match(/\r?\n?/)[0].length:this.yylloc.last_column+n[0].length},this.yytext+=n[0],this.match+=n[0],this.matches=n,this.yyleng=this.yytext.length,this.options.ranges&&(this.yylloc.range=[this.offset,this.offset+=this.yyleng]),this._more=false,this._backtrack=false,this._input=this._input.slice(n[0].length),this.matched+=n[0],d=this.performAction.call(this,this.yy,this,u,this.conditionStack[this.conditionStack.length-1]),this.done&&this._input&&(this.done=false),d)return d;if(this._backtrack){for(var e in p)this[e]=p[e];return  false}return  false},next:function(){if(this.done)return this.EOF;this._input||(this.done=true);var n,u,d,o;this._more||(this.yytext="",this.match="");for(var p=this._currentRules(),e=0;e<p.length;e++)if(d=this._input.match(this.rules[p[e]]),d&&(!u||d[0].length>u[0].length)){if(u=d,o=e,this.options.backtrack_lexer){if(n=this.test_match(d,p[e]),n!==false)return n;if(this._backtrack){u=false;continue}else return  false}else if(!this.options.flex)break}return u?(n=this.test_match(u,p[o]),n!==false?n:false):this._input===""?this.EOF:this.parseError("Lexical error on line "+(this.yylineno+1)+`. Unrecognized text.
`+this.showPosition(),{text:"",token:null,line:this.yylineno})},lex:function(){var u=this.next();return u||this.lex()},begin:function(u){this.conditionStack.push(u);},popState:function(){var u=this.conditionStack.length-1;return u>0?this.conditionStack.pop():this.conditionStack[0]},_currentRules:function(){return this.conditionStack.length&&this.conditionStack[this.conditionStack.length-1]?this.conditions[this.conditionStack[this.conditionStack.length-1]].rules:this.conditions.INITIAL.rules},topState:function(u){return u=this.conditionStack.length-1-Math.abs(u||0),u>=0?this.conditionStack[u]:"INITIAL"},pushState:function(u){this.begin(u);},stateStackSize:function(){return this.conditionStack.length},options:{"case-insensitive":true},performAction:function(u,d,o,p){switch(o){case 0:return this.begin("open_directive"),"open_directive";case 1:return this.begin("acc_title"),28;case 2:return this.popState(),"acc_title_value";case 3:return this.begin("acc_descr"),30;case 4:return this.popState(),"acc_descr_value";case 5:this.begin("acc_descr_multiline");break;case 6:this.popState();break;case 7:return "acc_descr_multiline_value";case 8:break;case 9:break;case 10:break;case 11:return 10;case 12:break;case 13:break;case 14:this.begin("href");break;case 15:this.popState();break;case 16:return 40;case 17:this.begin("callbackname");break;case 18:this.popState();break;case 19:this.popState(),this.begin("callbackargs");break;case 20:return 38;case 21:this.popState();break;case 22:return 39;case 23:this.begin("click");break;case 24:this.popState();break;case 25:return 37;case 26:return 4;case 27:return 19;case 28:return 20;case 29:return 21;case 30:return 22;case 31:return 23;case 32:return 25;case 33:return 24;case 34:return 26;case 35:return 12;case 36:return 13;case 37:return 14;case 38:return 15;case 39:return 16;case 40:return 17;case 41:return 18;case 42:return "date";case 43:return 27;case 44:return "accDescription";case 45:return 33;case 46:return 35;case 47:return 36;case 48:return ":";case 49:return 6;case 50:return "INVALID"}},rules:[/^(?:%%\{)/i,/^(?:accTitle\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*:\s*)/i,/^(?:(?!\n||)*[^\n]*)/i,/^(?:accDescr\s*\{\s*)/i,/^(?:[\}])/i,/^(?:[^\}]*)/i,/^(?:%%(?!\{)*[^\n]*)/i,/^(?:[^\}]%%*[^\n]*)/i,/^(?:%%*[^\n]*[\n]*)/i,/^(?:[\n]+)/i,/^(?:\s+)/i,/^(?:%[^\n]*)/i,/^(?:href[\s]+["])/i,/^(?:["])/i,/^(?:[^"]*)/i,/^(?:call[\s]+)/i,/^(?:\([\s]*\))/i,/^(?:\()/i,/^(?:[^(]*)/i,/^(?:\))/i,/^(?:[^)]*)/i,/^(?:click[\s]+)/i,/^(?:[\s\n])/i,/^(?:[^\s\n]*)/i,/^(?:gantt\b)/i,/^(?:dateFormat\s[^#\n;]+)/i,/^(?:inclusiveEndDates\b)/i,/^(?:topAxis\b)/i,/^(?:axisFormat\s[^#\n;]+)/i,/^(?:tickInterval\s[^#\n;]+)/i,/^(?:includes\s[^#\n;]+)/i,/^(?:excludes\s[^#\n;]+)/i,/^(?:todayMarker\s[^\n;]+)/i,/^(?:weekday\s+monday\b)/i,/^(?:weekday\s+tuesday\b)/i,/^(?:weekday\s+wednesday\b)/i,/^(?:weekday\s+thursday\b)/i,/^(?:weekday\s+friday\b)/i,/^(?:weekday\s+saturday\b)/i,/^(?:weekday\s+sunday\b)/i,/^(?:\d\d\d\d-\d\d-\d\d\b)/i,/^(?:title\s[^\n]+)/i,/^(?:accDescription\s[^#\n;]+)/i,/^(?:section\s[^\n]+)/i,/^(?:[^:\n]+)/i,/^(?::[^#\n;]+)/i,/^(?::)/i,/^(?:$)/i,/^(?:.)/i],conditions:{acc_descr_multiline:{rules:[6,7],inclusive:false},acc_descr:{rules:[4],inclusive:false},acc_title:{rules:[2],inclusive:false},callbackargs:{rules:[21,22],inclusive:false},callbackname:{rules:[18,19,20],inclusive:false},href:{rules:[15,16],inclusive:false},click:{rules:[24,25],inclusive:false},INITIAL:{rules:[0,1,3,5,8,9,10,11,12,13,14,17,23,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50],inclusive:true}}};return y})();g.lexer=v;function b(){this.yy={};}return b.prototype=g,g.Parser=b,new b})();Pt.parser=Pt;var Re=Pt;z.default.extend(Se.default);z.default.extend(Ee.default);z.default.extend(Me.default);var Q="",Bt="",jt,Ht="",ht=[],mt=[],Gt={},Xt=[],Tt=[],ot="",qt="",Ae=["active","done","crit","milestone"],Ut=[],kt=false,Zt=false,Qt="sunday",zt=0,Be=function(){Xt=[],Tt=[],ot="",Ut=[],gt=0,Rt=void 0,pt=void 0,W=[],Q="",Bt="",qt="",jt=void 0,Ht="",ht=[],mt=[],kt=false,Zt=false,zt=0,Gt={},jb(),Qt="sunday";},je=function(t){Bt=t;},He=function(){return Bt},Ge=function(t){jt=t;},Xe=function(){return jt},qe=function(t){Ht=t;},Ue=function(){return Ht},Ze=function(t){Q=t;},Qe=function(){kt=true;},Je=function(){return kt},Ke=function(){Zt=true;},$e=function(){return Zt},ti=function(t){qt=t;},ei=function(){return qt},ii=function(){return Q},si=function(t){ht=t.toLowerCase().split(/[\s,]+/);},ni=function(){return ht},ri=function(t){mt=t.toLowerCase().split(/[\s,]+/);},ai=function(){return mt},oi=function(){return Gt},ci=function(t){ot=t,Xt.push(t);},li=function(){return Xt},ui=function(){let t=_e(),i=10,r=0;for(;!t&&r<i;)t=_e(),r++;return Tt=W,Tt},Le=function(t,i,r,a){return a.includes(t.format(i.trim()))?false:t.isoWeekday()>=6&&r.includes("weekends")||r.includes(t.format("dddd").toLowerCase())?true:r.includes(t.format(i.trim()))},di=function(t){Qt=t;},fi=function(){return Qt},Ie=function(t,i,r,a){if(!r.length||t.manualEndTime)return;let s;t.startTime instanceof Date?s=(0, z.default)(t.startTime):s=(0, z.default)(t.startTime,i,true),s=s.add(1,"d");let f;t.endTime instanceof Date?f=(0, z.default)(t.endTime):f=(0, z.default)(t.endTime,i,true);let[h,_]=hi(s,f,i,r,a);t.endTime=h.toDate(),t.renderEndTime=_;},hi=function(t,i,r,a,s){let f=false,h=null;for(;t<=i;)f||(h=i.toDate()),f=Le(t,r,a,s),f&&(i=i.add(1,"d")),t=t.add(1,"d");return [i,h]},Nt=function(t,i,r){r=r.trim();let s=/^after\s+(?<ids>[\d\w- ]+)/.exec(r);if(s!==null){let h=null;for(let x of s.groups.ids.split(" ")){let A=rt(x);A!==void 0&&(!h||A.endTime>h.endTime)&&(h=A);}if(h)return h.endTime;let _=new Date;return _.setHours(0,0,0,0),_}let f=(0, z.default)(r,i.trim(),true);if(f.isValid())return f.toDate();{Ka.debug("Invalid date:"+r),Ka.debug("With date format:"+i.trim());let h=new Date(r);if(h===void 0||isNaN(h.getTime())||h.getFullYear()<-1e4||h.getFullYear()>1e4)throw new Error("Invalid date:"+r);return h}},Ye=function(t){let i=/^(\d+(?:\.\d+)?)([Mdhmswy]|ms)$/.exec(t.trim());return i!==null?[Number.parseFloat(i[1]),i[2]]:[NaN,"ms"]},Fe=function(t,i,r,a=false){r=r.trim();let f=/^until\s+(?<ids>[\d\w- ]+)/.exec(r);if(f!==null){let C=null;for(let H of f.groups.ids.split(" ")){let P=rt(H);P!==void 0&&(!C||P.startTime<C.startTime)&&(C=P);}if(C)return C.startTime;let F=new Date;return F.setHours(0,0,0,0),F}let h=(0, z.default)(r,i.trim(),true);if(h.isValid())return a&&(h=h.add(1,"d")),h.toDate();let _=(0, z.default)(t),[x,A]=Ye(r);if(!Number.isNaN(x)){let C=_.add(x,A);C.isValid()&&(_=C);}return _.toDate()},gt=0,at=function(t){return t===void 0?(gt=gt+1,"task"+gt):t},mi=function(t,i){let r;i.substr(0,1)===":"?r=i.substr(1,i.length):r=i;let a=r.split(","),s={};Pe(a,s,Ae);for(let h=0;h<a.length;h++)a[h]=a[h].trim();let f="";switch(a.length){case 1:s.id=at(),s.startTime=t.endTime,f=a[0];break;case 2:s.id=at(),s.startTime=Nt(void 0,Q,a[0]),f=a[1];break;case 3:s.id=at(a[0]),s.startTime=Nt(void 0,Q,a[1]),f=a[2];break}return f&&(s.endTime=Fe(s.startTime,Q,f,kt),s.manualEndTime=(0, z.default)(f,"YYYY-MM-DD",true).isValid(),Ie(s,Q,mt,ht)),s},ki=function(t,i){let r;i.substr(0,1)===":"?r=i.substr(1,i.length):r=i;let a=r.split(","),s={};Pe(a,s,Ae);for(let f=0;f<a.length;f++)a[f]=a[f].trim();switch(a.length){case 1:s.id=at(),s.startTime={type:"prevTaskEnd",id:t},s.endTime={data:a[0]};break;case 2:s.id=at(),s.startTime={type:"getStartDate",startData:a[0]},s.endTime={data:a[1]};break;case 3:s.id=at(a[0]),s.startTime={type:"getStartDate",startData:a[1]},s.endTime={data:a[2]};break}return s},Rt,pt,W=[],We={},yi=function(t,i){let r={section:ot,type:ot,processed:false,manualEndTime:false,renderEndTime:null,raw:{data:i},task:t,classes:[]},a=ki(pt,i);r.raw.startTime=a.startTime,r.raw.endTime=a.endTime,r.id=a.id,r.prevTaskId=pt,r.active=a.active,r.done=a.done,r.crit=a.crit,r.milestone=a.milestone,r.order=zt,zt++;let s=W.push(r);pt=r.id,We[r.id]=s-1;},rt=function(t){let i=We[t];return W[i]},gi=function(t,i){let r={section:ot,type:ot,description:t,task:t,classes:[]},a=mi(Rt,i);r.startTime=a.startTime,r.endTime=a.endTime,r.id=a.id,r.active=a.active,r.done=a.done,r.crit=a.crit,r.milestone=a.milestone,Rt=r,Tt.push(r);},_e=function(){let t=function(r){let a=W[r],s="";switch(W[r].raw.startTime.type){case "prevTaskEnd":{let f=rt(a.prevTaskId);a.startTime=f.endTime;break}case "getStartDate":s=Nt(void 0,Q,W[r].raw.startTime.startData),s&&(W[r].startTime=s);break}return W[r].startTime&&(W[r].endTime=Fe(W[r].startTime,Q,W[r].raw.endTime.data,kt),W[r].endTime&&(W[r].processed=true,W[r].manualEndTime=(0, z.default)(W[r].raw.endTime.data,"YYYY-MM-DD",true).isValid(),Ie(W[r],Q,mt,ht))),W[r].processed},i=true;for(let[r,a]of W.entries())t(r),i=i&&a.processed;return i},pi=function(t,i){let r=i;rb().securityLevel!=="loose"&&(r=(0, Ce.sanitizeUrl)(i)),t.split(",").forEach(function(a){rt(a)!==void 0&&(Ve(a,()=>{window.open(r,"_self");}),Gt[a]=r);}),Oe(t,"clickable");},Oe=function(t,i){t.split(",").forEach(function(r){let a=rt(r);a!==void 0&&a.classes.push(i);});},Ti=function(t,i,r){if(rb().securityLevel!=="loose"||i===void 0)return;let a=[];if(typeof r=="string"){a=r.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);for(let f=0;f<a.length;f++){let h=a[f].trim();h.charAt(0)==='"'&&h.charAt(h.length-1)==='"'&&(h=h.substr(1,h.length-2)),a[f]=h;}}a.length===0&&a.push(t),rt(t)!==void 0&&Ve(t,()=>{eb.runFunc(i,...a);});},Ve=function(t,i){Ut.push(function(){let r=document.querySelector(`[id="${t}"]`);r!==null&&r.addEventListener("click",function(){i();});},function(){let r=document.querySelector(`[id="${t}-text"]`);r!==null&&r.addEventListener("click",function(){i();});});},bi=function(t,i,r){t.split(",").forEach(function(a){Ti(a,i,r);}),Oe(t,"clickable");},vi=function(t){Ut.forEach(function(i){i(t);});},xi={getConfig:()=>rb().gantt,clear:Be,setDateFormat:Ze,getDateFormat:ii,enableInclusiveEndDates:Qe,endDatesAreInclusive:Je,enableTopAxis:Ke,topAxisEnabled:$e,setAxisFormat:je,getAxisFormat:He,setTickInterval:Ge,getTickInterval:Xe,setTodayMarker:qe,getTodayMarker:Ue,setAccTitle:kb,getAccTitle:lb,setDiagramTitle:ob,getDiagramTitle:pb,setDisplayMode:ti,getDisplayMode:ei,setAccDescription:mb,getAccDescription:nb,addSection:ci,getSections:li,getTasks:ui,addTask:yi,findTaskById:rt,addTaskOrg:gi,setIncludes:si,getIncludes:ni,setExcludes:ri,getExcludes:ai,setClickEvent:bi,setLink:pi,getLinks:oi,bindFunctions:vi,parseDuration:Ye,isInvalidDate:Le,setWeekday:di,getWeekday:fi};function Pe(t,i,r){let a=true;for(;a;)a=false,r.forEach(function(s){let f="^\\s*"+s+"\\s*$",h=new RegExp(f);t[0].match(h)&&(i[s]=true,t.shift(1),a=true);});}var wi=function(){Ka.debug("Something is calling, setConf, remove the call");},De={monday:D,tuesday:E,wednesday:F,thursday:G,friday:H,saturday:I,sunday:C},_i=(t,i)=>{let r=[...t].map(()=>-1/0),a=[...t].sort((f,h)=>f.startTime-h.startTime||f.order-h.order),s=0;for(let f of a)for(let h=0;h<r.length;h++)if(f.startTime>=r[h]){r[h]=f.endTime,f.order=h+i,h>s&&(s=h);break}return s},K,Di=function(t,i,r$1,a$1){let s$1=rb().gantt,f=rb().securityLevel,h;f==="sandbox"&&(h=a("#i"+i));let _=f==="sandbox"?a(h.nodes()[0].contentDocument.body):a("body"),x$1=f==="sandbox"?h.nodes()[0].contentDocument:document,A$1=x$1.getElementById(i);K=A$1.parentElement.offsetWidth,K===void 0&&(K=1200),s$1.useWidth!==void 0&&(K=s$1.useWidth);let C=a$1.db.getTasks(),F=[];for(let g of C)F.push(g.type);F=j(F);let H={},P=2*s$1.topPadding;if(a$1.db.getDisplayMode()==="compact"||s$1.displayMode==="compact"){let g={};for(let b of C)g[b.section]===void 0?g[b.section]=[b]:g[b.section].push(b);let v=0;for(let b of Object.keys(g)){let y=_i(g[b],v)+1;v+=y,P+=y*(s$1.barHeight+s$1.barGap),H[b]=y;}}else {P+=C.length*(s$1.barHeight+s$1.barGap);for(let g of F)H[g]=C.filter(v=>v.type===g).length;}A$1.setAttribute("viewBox","0 0 "+K+" "+P);let k=_.select(`[id="${i}"]`),m=L().domain([q(C,function(g){return g.startTime}),p(C,function(g){return g.endTime})]).rangeRound([0,K-s$1.leftPadding-s$1.rightPadding]);function M(g,v){let b=g.startTime,y=v.startTime,n=0;return b>y?n=1:b<y&&(n=-1),n}C.sort(M),L$1(C,K,P),hb(k,P,K,s$1.useMaxWidth),k.append("text").text(a$1.db.getDiagramTitle()).attr("x",K/2).attr("y",s$1.titleTopMargin).attr("class","titleText");function L$1(g,v,b){let y=s$1.barHeight,n=y+s$1.barGap,u=s$1.topPadding,d$1=s$1.leftPadding,o=w().domain([0,F.length]).range(["#00B9FA","#F95002"]).interpolate(d);R(n,u,d$1,v,b,g,a$1.db.getExcludes(),a$1.db.getIncludes()),U(d$1,u,v,b),N(g,n,u,d$1,y,o,v),X(n,u),B$1(d$1,u,v,b);}function N(g,v,b,y,n,u,d){let p=[...new Set(g.map(l=>l.order))].map(l=>g.find(T=>T.order===l));k.append("g").selectAll("rect").data(p).enter().append("rect").attr("x",0).attr("y",function(l,T){return T=l.order,T*v+b-2}).attr("width",function(){return d-s$1.rightPadding/2}).attr("height",v).attr("class",function(l){for(let[T,I]of F.entries())if(l.type===I)return "section section"+T%s$1.numberSectionStyles;return "section section0"});let e=k.append("g").selectAll("rect").data(g).enter(),Y=a$1.db.getLinks();if(e.append("rect").attr("id",function(l){return l.id}).attr("rx",3).attr("ry",3).attr("x",function(l){return l.milestone?m(l.startTime)+y+.5*(m(l.endTime)-m(l.startTime))-.5*n:m(l.startTime)+y}).attr("y",function(l,T){return T=l.order,T*v+b}).attr("width",function(l){return l.milestone?n:m(l.renderEndTime||l.endTime)-m(l.startTime)}).attr("height",n).attr("transform-origin",function(l,T){return T=l.order,(m(l.startTime)+y+.5*(m(l.endTime)-m(l.startTime))).toString()+"px "+(T*v+b+.5*n).toString()+"px"}).attr("class",function(l){let T="task",I="";l.classes.length>0&&(I=l.classes.join(" "));let D=0;for(let[w,E]of F.entries())l.type===E&&(D=w%s$1.numberSectionStyles);let S="";return l.active?l.crit?S+=" activeCrit":S=" active":l.done?l.crit?S=" doneCrit":S=" done":l.crit&&(S+=" crit"),S.length===0&&(S=" task"),l.milestone&&(S=" milestone "+S),S+=D,S+=" "+I,T+S}),e.append("text").attr("id",function(l){return l.id+"-text"}).text(function(l){return l.task}).attr("font-size",s$1.fontSize).attr("x",function(l){let T=m(l.startTime),I=m(l.renderEndTime||l.endTime);l.milestone&&(T+=.5*(m(l.endTime)-m(l.startTime))-.5*n),l.milestone&&(I=T+n);let D=this.getBBox().width;return D>I-T?I+D+1.5*s$1.leftPadding>d?T+y-5:I+y+5:(I-T)/2+T+y}).attr("y",function(l,T){return T=l.order,T*v+s$1.barHeight/2+(s$1.fontSize/2-2)+b}).attr("text-height",n).attr("class",function(l){let T=m(l.startTime),I=m(l.endTime);l.milestone&&(I=T+n);let D=this.getBBox().width,S="";l.classes.length>0&&(S=l.classes.join(" "));let w=0;for(let[$,tt]of F.entries())l.type===tt&&(w=$%s$1.numberSectionStyles);let E="";return l.active&&(l.crit?E="activeCritText"+w:E="activeText"+w),l.done?l.crit?E=E+" doneCritText"+w:E=E+" doneText"+w:l.crit&&(E=E+" critText"+w),l.milestone&&(E+=" milestoneText"),D>I-T?I+D+1.5*s$1.leftPadding>d?S+" taskTextOutsideLeft taskTextOutside"+w+" "+E:S+" taskTextOutsideRight taskTextOutside"+w+" "+E+" width-"+D:S+" taskText taskText"+w+" "+E+" width-"+D}),rb().securityLevel==="sandbox"){let l;l=a("#i"+i);let T=l.nodes()[0].contentDocument;e.filter(function(I){return Y[I.id]!==void 0}).each(function(I){var D=T.querySelector("#"+I.id),S=T.querySelector("#"+I.id+"-text");let w=D.parentNode;var E=T.createElement("a");E.setAttribute("xlink:href",Y[I.id]),E.setAttribute("target","_top"),w.appendChild(E),E.appendChild(D),E.appendChild(S);});}}function R(g,v,b,y,n,u,d,o){if(d.length===0&&o.length===0)return;let p,e;for(let{startTime:D,endTime:S}of u)(p===void 0||D<p)&&(p=D),(e===void 0||S>e)&&(e=S);if(!p||!e)return;if((0, z.default)(e).diff((0, z.default)(p),"year")>5){Ka.warn("The difference between the min and max time is more than 5 years. This will cause performance issues. Skipping drawing exclude days.");return}let Y=a$1.db.getDateFormat(),c=[],l=null,T=(0, z.default)(p);for(;T.valueOf()<=e;)a$1.db.isInvalidDate(T,Y,d,o)?l?l.end=T:l={start:T,end:T}:l&&(c.push(l),l=null),T=T.add(1,"d");k.append("g").selectAll("rect").data(c).enter().append("rect").attr("id",function(D){return "exclude-"+D.start.format("YYYY-MM-DD")}).attr("x",function(D){return m(D.start)+b}).attr("y",s$1.gridLineStartPadding).attr("width",function(D){let S=D.end.add(1,"day");return m(S)-m(D.start)}).attr("height",n-v-s$1.gridLineStartPadding).attr("transform-origin",function(D,S){return (m(D.start)+b+.5*(m(D.end)-m(D.start))).toString()+"px "+(S*g+.5*n).toString()+"px"}).attr("class","exclude-range");}function U(g,v,b,y$1){let n=s(m).tickSize(-y$1+v+s$1.gridLineStartPadding).tickFormat(K$1(a$1.db.getAxisFormat()||s$1.axisFormat||"%Y-%m-%d")),d=/^([1-9]\d*)(millisecond|second|minute|hour|day|week|month)$/.exec(a$1.db.getTickInterval()||s$1.tickInterval);if(d!==null){let o=d[1],p=d[2],e=a$1.db.getWeekday()||s$1.weekday;switch(p){case "millisecond":n.ticks(x.every(o));break;case "second":n.ticks(y.every(o));break;case "minute":n.ticks(z$1.every(o));break;case "hour":n.ticks(A.every(o));break;case "day":n.ticks(B.every(o));break;case "week":n.ticks(De[e].every(o));break;case "month":n.ticks(J.every(o));break}}if(k.append("g").attr("class","grid").attr("transform","translate("+g+", "+(y$1-50)+")").call(n).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10).attr("dy","1em"),a$1.db.topAxisEnabled()||s$1.topAxis){let o=r(m).tickSize(-y$1+v+s$1.gridLineStartPadding).tickFormat(K$1(a$1.db.getAxisFormat()||s$1.axisFormat||"%Y-%m-%d"));if(d!==null){let p=d[1],e=d[2],Y=a$1.db.getWeekday()||s$1.weekday;switch(e){case "millisecond":o.ticks(x.every(p));break;case "second":o.ticks(y.every(p));break;case "minute":o.ticks(z$1.every(p));break;case "hour":o.ticks(A.every(p));break;case "day":o.ticks(B.every(p));break;case "week":o.ticks(De[Y].every(p));break;case "month":o.ticks(J.every(p));break}}k.append("g").attr("class","grid").attr("transform","translate("+g+", "+v+")").call(o).selectAll("text").style("text-anchor","middle").attr("fill","#000").attr("stroke","none").attr("font-size",10);}}function X(g,v){let b=0,y=Object.keys(H).map(n=>[n,H[n]]);k.append("g").selectAll("text").data(y).enter().append(function(n){let u=n[0].split(Sa.lineBreakRegex),d=-(u.length-1)/2,o=x$1.createElementNS("http://www.w3.org/2000/svg","text");o.setAttribute("dy",d+"em");for(let[p,e]of u.entries()){let Y=x$1.createElementNS("http://www.w3.org/2000/svg","tspan");Y.setAttribute("alignment-baseline","central"),Y.setAttribute("x","10"),p>0&&Y.setAttribute("dy","1em"),Y.textContent=e,o.appendChild(Y);}return o}).attr("x",10).attr("y",function(n,u){if(u>0)for(let d=0;d<u;d++)return b+=y[u-1][1],n[1]*g/2+b*g+v;else return n[1]*g/2+v}).attr("font-size",s$1.sectionFontSize).attr("class",function(n){for(let[u,d]of F.entries())if(n[0]===d)return "sectionTitle sectionTitle"+u%s$1.numberSectionStyles;return "sectionTitle"});}function B$1(g,v,b,y){let n=a$1.db.getTodayMarker();if(n==="off")return;let u=k.append("g").attr("class","today"),d=new Date,o=u.append("line");o.attr("x1",m(d)+g).attr("x2",m(d)+g).attr("y1",s$1.titleTopMargin).attr("y2",y-s$1.titleTopMargin).attr("class","today"),n!==""&&o.attr("style",n.replace(/,/g,";"));}function j(g){let v={},b=[];for(let y=0,n=g.length;y<n;++y)Object.prototype.hasOwnProperty.call(v,g[y])||(v[g[y]]=true,b.push(g[y]));return b}},Ci={setConf:wi,draw:Di},Si=t=>`
  .mermaid-main-font {
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .exclude-range {
    fill: ${t.excludeBkgColor};
  }

  .section {
    stroke: none;
    opacity: 0.2;
  }

  .section0 {
    fill: ${t.sectionBkgColor};
  }

  .section2 {
    fill: ${t.sectionBkgColor2};
  }

  .section1,
  .section3 {
    fill: ${t.altSectionBkgColor};
    opacity: 0.2;
  }

  .sectionTitle0 {
    fill: ${t.titleColor};
  }

  .sectionTitle1 {
    fill: ${t.titleColor};
  }

  .sectionTitle2 {
    fill: ${t.titleColor};
  }

  .sectionTitle3 {
    fill: ${t.titleColor};
  }

  .sectionTitle {
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }


  /* Grid and axis */

  .grid .tick {
    stroke: ${t.gridColor};
    opacity: 0.8;
    shape-rendering: crispEdges;
  }

  .grid .tick text {
    font-family: ${t.fontFamily};
    fill: ${t.textColor};
  }

  .grid path {
    stroke-width: 0;
  }


  /* Today line */

  .today {
    fill: none;
    stroke: ${t.todayLineColor};
    stroke-width: 2px;
  }


  /* Task styling */

  /* Default task */

  .task {
    stroke-width: 2;
  }

  .taskText {
    text-anchor: middle;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideRight {
    fill: ${t.taskTextDarkColor};
    text-anchor: start;
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }

  .taskTextOutsideLeft {
    fill: ${t.taskTextDarkColor};
    text-anchor: end;
  }


  /* Special case clickable */

  .task.clickable {
    cursor: pointer;
  }

  .taskText.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideLeft.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }

  .taskTextOutsideRight.clickable {
    cursor: pointer;
    fill: ${t.taskTextClickableColor} !important;
    font-weight: bold;
  }


  /* Specific task settings for the sections*/

  .taskText0,
  .taskText1,
  .taskText2,
  .taskText3 {
    fill: ${t.taskTextColor};
  }

  .task0,
  .task1,
  .task2,
  .task3 {
    fill: ${t.taskBkgColor};
    stroke: ${t.taskBorderColor};
  }

  .taskTextOutside0,
  .taskTextOutside2
  {
    fill: ${t.taskTextOutsideColor};
  }

  .taskTextOutside1,
  .taskTextOutside3 {
    fill: ${t.taskTextOutsideColor};
  }


  /* Active task */

  .active0,
  .active1,
  .active2,
  .active3 {
    fill: ${t.activeTaskBkgColor};
    stroke: ${t.activeTaskBorderColor};
  }

  .activeText0,
  .activeText1,
  .activeText2,
  .activeText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Completed task */

  .done0,
  .done1,
  .done2,
  .done3 {
    stroke: ${t.doneTaskBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
  }

  .doneText0,
  .doneText1,
  .doneText2,
  .doneText3 {
    fill: ${t.taskTextDarkColor} !important;
  }


  /* Tasks on the critical line */

  .crit0,
  .crit1,
  .crit2,
  .crit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.critBkgColor};
    stroke-width: 2;
  }

  .activeCrit0,
  .activeCrit1,
  .activeCrit2,
  .activeCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.activeTaskBkgColor};
    stroke-width: 2;
  }

  .doneCrit0,
  .doneCrit1,
  .doneCrit2,
  .doneCrit3 {
    stroke: ${t.critBorderColor};
    fill: ${t.doneTaskBkgColor};
    stroke-width: 2;
    cursor: pointer;
    shape-rendering: crispEdges;
  }

  .milestone {
    transform: rotate(45deg) scale(0.8,0.8);
  }

  .milestoneText {
    font-style: italic;
  }
  .doneCritText0,
  .doneCritText1,
  .doneCritText2,
  .doneCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .activeCritText0,
  .activeCritText1,
  .activeCritText2,
  .activeCritText3 {
    fill: ${t.taskTextDarkColor} !important;
  }

  .titleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${t.titleColor||t.textColor};
    font-family: var(--mermaid-font-family, "trebuchet ms", verdana, arial, sans-serif);
  }
`,Ei=Si,Oi={parser:Re,db:xi,renderer:Ci,styles:Ei};export{Oi as diagram};//# sourceMappingURL=ganttDiagram-a2739b55-ZLD5JRJD.js.map
//# sourceMappingURL=ganttDiagram-a2739b55-ZLD5JRJD.js.map