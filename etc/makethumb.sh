## iusage: ./script 9 thumbnails
homedir=`echo $3`
size=`echo $1`
type=`echo $2`
cd $homedir/images
for directory in `ls -d *`
do
	cd $directory
	dest=`echo $homedir/production/$type/$directory`
	mkdir $dest 2> /dev/null
	for img in `ls *.JPG *.jpg 2> /dev/null`
	do
		lc=`echo $img  | tr '[A-Z]' '[a-z]'`
  		if [ $lc != $img ]; then
    			mv -i $img $lc
  		fi
		sourcepath=`echo $homedir/images/$directory/$lc`
		fullpath=`echo $dest/$lc`
				
		if [ ! -e $fullpath ]; then
	          echo "Running" $sourcepath " -to " $fullpath >> $homedir/store/i.log
        	  echo "Running"  $fullpath
		  /usr/bin/X11/convert -resize $size $sourcepath $fullpath 2>> $homedir/i.log
		else
	              echo "Already done " $fullpath >> $homedir/store/i.log
		fi
	done
	cd ..
done
