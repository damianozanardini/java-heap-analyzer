CHORD = ${HOME}/Systems/chord-src-2.1/

dist:
	mkdir dist
	cp ${CHORD}build.xml dist
	cp ${CHORD}chord.jar dist
	cp ${CHORD}chord.properties dist
	cp ${CHORD}COPYING dist
	cp distREADME dist/README
	mkdir dist/examples
	cp -R ${HOME}/programming/chord_reach_examples/cyclicTree dist/examples
	cp -R ${HOME}/programming/chord_reach_examples/linkedList dist/examples
	tar cvf - dist | gzip > dist.tar.gz
	rm -rf dist
	mv dist.tar.gz ${HOME}/webPage/reachCycle

