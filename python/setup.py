from setuptools import setup, find_packages

from buildutils.install import install

setup(
    name='pythonmatsim',
    version='0.1a1',
    package_dir={'pythonmatsim': './',
                 '': 'generated-code/'},
    packages=find_packages(exclude=('buildutils', 'test')),
    include_package_data=True,
    url='',
    license='GNU GPL 3.0',
    author='Thibaut Dubernet',
    author_email='thibaut.dubernet@ivt.baug.ethz.ch',
    description='A package to use MATSim from Python',
    install_requires=[
        'JPype1==0.6.3',
    ],
    cmdclass={
      'install': install
    },
)
