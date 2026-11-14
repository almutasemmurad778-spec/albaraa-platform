import sys
import os
import unittest
sys.path.insert(0,os.path.dirname(os.path.dirname(__file__)))
from bridge import Binance, BridgeError, code, number

class BridgeTests(unittest.TestCase):
    def test_codes(self):
        self.assertEqual(code('btcusdt'),'BTCUSDT')
        with self.assertRaises(BridgeError):
            code('../../bad')
    def test_amounts(self):
        self.assertEqual(number('0.001'), '0.001')
        for amount in ['0', '-1','NaN','Infinity','abc']:
            with self.assertRaises(BridgeError):
                number(amount)
    def test_spot_preview_does_not_place_order(self):
        b=Binance('sample','sample')
        calls=[]
        def fake(method,path,params=None,signed=False):
            calls.append(path)
            if path=='/api/v3/ticker/price':return {'price':'100'}
            if path=='/api/v3/order/test':return {}
            raise AssertionError('Unexpected path: '+path)
        b.call=fake
        ret=b.spot_order('BTCUSDT','BUY','0.02',None,False,10,lambda *_:False)
        self.assertEqual(ret['status'],'TEST_ONLY')
        self.assertNotIn('/api/v3/order',calls)
    def test_declined_trade_does_not_place(self):
        b=Binance('sample','sample')
        paths=[]
        def fake(method,path,params=None,signed=False):
            paths.append(path)
            if path=='/api/v3/ticker/price':return {'price':'100'}
            if path=='/api/v3/order/test':return {}
            raise AssertionError('Trade should not happen')
        b.call=fake
        ret=b.spot_order('BTCUSDT','BUY','0.02',None,True,10,lambda *_:False)
        self.assertEqual(ret['status'],'CANCELLED_BY_USER')
        self.assertNotIn('/api/v3/order',paths)
    def test_cap(self):
        b=Binance()
        with self.assertRaises(BridgeError):
            b.cap_check('101','100')

if __name__=='__main__':unittest.main()
