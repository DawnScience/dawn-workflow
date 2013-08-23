from numpy import *

def run(x, y, p, q, I0, It, **kwargs):
    z = y*x
    r = p+q
    l = I0/It
    return {'z': z, 'r': r, 'l': l}

