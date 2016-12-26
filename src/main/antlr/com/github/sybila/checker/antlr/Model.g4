grammar Model;

@header {
package com.github.sybila.checker.antlr;
}

root: fullStop? (statement fullStop)*;

fullStop : NEWLINE+ | EOF | ';';

statement : ':params' param (',' param)*                            #params
          | ':states' ('(' NUM ')')? state (',' state)*             #states
          | ':edges' edge (',' edge)*                               #edges
          | ':verify' STRING                                        #verify
          | ':atom' STRING '=' '[' (stateParams (',' stateParams)*)? ']'       #atom
          | ':assert' STRING '==' '[' (stateParams (',' stateParams)*)? ']'    #assert
          ;

edge : state '-' ('(' param (',' param)* ')')? '>' STRING? state;

stateParams : state | state'(' param (',' param)* ')';

state: NAME | NUM;
param: NAME | NUM;

/* Literals */


NUM: [0-9]+;

NAME : [_a-zA-Z]+[_a-zA-Z0-9]*;

STRING : ["].+?["];

NEWLINE : '\r'?'\n';

WS : [\t ]+ -> channel(HIDDEN) ;

Block_comment : '/*' (Block_comment|.)*? '*/' -> channel(HIDDEN) ; // nesting allow
C_Line_comment : '//' .*? ('\n' | EOF) -> channel(HIDDEN) ;
Python_Line_comment : '#' .*? ('\n' | EOF) -> channel(HIDDEN) ;