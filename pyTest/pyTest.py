import math
import pyaudio
import Image

def scale(n, minor = False):
	ret = 0
	threshold = 3
	if minor:
		threshold = 2

	while (n < 0):
		n += 7
		ret -= 12
	while (n >= 7):
		n -= 7
		ret += 12

	ret = ret + n * 2
	if n >= threshold:	# if minor, set to 2
		ret -= 1
	if minor and n == 5:
		ret -= 1
	return ret

def addSound(semidiff, length, minor = False):
	bitrate = 16000
	wavedata = ""
	if semidiff > -255:
		octave = scale(semidiff, minor) / 12.0
		freq = 440 * (2 ** octave)
		wavelengths = int(round(length * freq))
		nFrames = int(math.floor(wavelengths * bitrate / float(freq)))
		cFlot = math.pi * 2 * freq / float(bitrate)

		for x in xrange(nFrames):
			wavedata = wavedata + chr(int(math.sin(x * cFlot)*127+128))
	else:
		for x in xrange(int(round(length * bitrate))):
			wavedata = wavedata + chr(128)
	return wavedata

def parseImg(img, scanline):
	pixels = img.load()
	pixelArray = []
	blockWidth = 5
	for x in xrange(img.size[0] / blockWidth):
		pixel = [0, 0, 0]
		for y in xrange(blockWidth):
			for z in xrange(3):
				pixel[z] += pixels[x * blockWidth + y, scanline][z]
		for z in xrange(3):
			pixel[z] //= blockWidth
		pixelArray.append(pixel)
		print "%d - %d, %d, %d" % (x, pixel[0], pixel[1], pixel[2])
	return pixelArray

def mapReduce(arr, fmap, fred):
	ret = 0
	for idx in xrange(len(arr)):
		if idx == 0:
			ret = fmap(arr[idx])
		else:
			ret = fred(ret, fmap(arr[idx]))

	return ret

def addEffects(pixelArray, x):
	wavedata = ""
	rPrev = False
	for idx in xrange(len(pixelArray)):
		p = pixelArray[idx]
		diff = 0
		length = 0.07
		blockWidth = 1
		if x == 0:		# soprano
			blockWidth = 2
			fmap = pmax
			fred = max
			diff = 7
		elif x == 1:
			blockWidth = 4
			fmap = pavg
			fred = lambda x, y: x + (y // 4)
			diff = 0
		elif x == 2:	# bass
			blockWidth = 8
			fmap = pmin
			fred = min
			diff = -7
		if (idx % blockWidth != 0):
			continue
		p = mapReduce(pixelArray[idx:idx + blockWidth], fmap, fred)
		length *= blockWidth
		diff = int(round((p - 127) / 10.0)) + diff

		if blockWidth > 1 and idx + blockWidth < len(pixelArray) and (fmap(pixelArray[idx]) == fmap(pixelArray[idx + blockWidth - 1])):
			diff = -255

		wavedata = wavedata + addSound(diff, length)
	return wavedata
		

def playImage(filename, scanline = -1, channel = -1):
	PyAudio = pyaudio.PyAudio
	BITRATE = 16000
	p = PyAudio()
	stream = p.open(format = p.get_format_from_width(1), 
					channels = 1,
					rate = BITRATE,
					output = True)
	stream.bitrate = BITRATE

	img = Image.open(filename)
	
	channels = 3
	values = []
	channelLen = 1
	if scanline == -1:
		scanline = img.size[1] // 2
	pixelArray = parseImg(img, scanline)
	for x in xrange(channels):
		values.append(addEffects(pixelArray, x))
		if len(values[x]) > channelLen:
			channelLen = len(values[x])

	wavedata = ""
	for idx in xrange(channelLen):
		sum = 0
		for x in xrange(channels):
			if idx < len(values[x]):
				sum += ord(values[x][idx])
		if channel != -1 and idx < len(values[channel]):
			channels = 1
			sum = ord(values[channel][idx])
		wavedata = wavedata + chr(int(sum // channels))

	stream.write(wavedata)
	stream.stop_stream()
	stream.close()
	p.terminate()

def pavg(pix):
	return (pix[0] + pix[1] + pix[2]) / 3.0

def pmin(pix):
	return min(pix[0], pix[1], pix[2])

def pmax(pix):
	return max(pix[0], pix[1], pix[2])

def scanAvg(sl):
	p = [0, 0, 0]
	for x in sl:
		for y in xrange(3):
			p[y] += x[y]
	for y in xrange(3):
		p[y] //= len(sl)
	return p

def outp(filename, scanline = -1):
	img = Image.open(filename)
	if scanline == -1:
		scanline = img.size[1] // 2
	pixelArray = parseImg(img, scanline)
	for y in xrange(len(pixelArray)):
		x = pixelArray[y]
		dx = [0, 0, 0]
		if y > 0:
			dx = pixelArray[y - 1][:]
		for z in xrange(3):
			dx[z] = x[z] - dx[z]
		print "%d, %d, %d = %d, %d, %d | %d, %d, %d" % (x[0], x[1], x[2], pavg(x), pmin(x), pmax(x), dx[0], dx[1], dx[2])

	p = scanAvg(pixelArray)
	print "Average: %d, %d, %d = %d, %d, %d" % (p[0], p[1], p[2], pavg(p), pmin(p), pmax(p))

	return pixelArray