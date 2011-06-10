rm -f /home/wspublisher/accounts-DEV/dreamllama/store/i.log

## iusage: ./script 9 thumbnails
homedir=`echo /home/wspublisher/accounts-DEV/dreamllama`
thumbsize=`echo 140x186`
medsize=`echo 200x267`
largesize=`echo 400x534`

echo Starting

cd $homedir/upload/imageinput
	

	for img in `ls *.JPG *.jpg 2> /dev/null`
	do
		##Move to lower case
		lc=`echo $img  | tr '[A-Z]' '[a-z]'`
  		if [ $lc != $img ]; then
    			mv -f $img $lc
  		fi
		sourcepath=`echo $homedir/upload/imageinput/$lc`
 		dest=`echo $homedir/production/thumbnails/$lc`

		##make thumbnails
		if /usr/bin/X11/convert -geometry $thumbsize $sourcepath $dest  >> $homedir/store/i.log; then
	                ##crop thumbnail  
		       /usr/bin/X11/convert -crop 100x133+17+10 $dest $dest 2>> $homedir/store/i.log
		else
			echo Could not process $sourcepath exiting >> $homedir/store/i.log
			exit -1
		fi
		
		#make medium
		dest=`echo $homedir/production/medium/$lc`
		/usr/bin/X11/convert -geometry $medsize $sourcepath $dest
		
		#make large
		dest=`echo $homedir/production/large/$lc`
		/usr/bin/X11/convert -geometry $largesize $sourcepath $dest

		#move to images dir
		mv -f $sourcepath $homedir/images 2>> $homedir/store/i.log 
		echo Completed $sourcepath  >> $homedir/store/i.log

	done
echo "Finished all"  >> $homedir/store/i.log
