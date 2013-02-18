#!/bin/sh
 
git filter-branch --env-filter '
 
an="James Hughes"
am="dboyzetown@gmail.com"
cn="James Hughes"
cm="dboyzetown@gmail.com"
 
export GIT_AUTHOR_NAME="$an"
export GIT_AUTHOR_EMAIL="$am"
export GIT_COMMITTER_NAME="$cn"
export GIT_COMMITTER_EMAIL="$cm"
'
