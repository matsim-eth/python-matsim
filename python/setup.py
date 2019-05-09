from setuptools import setup

from buildutils.codegeneration import JavaWrapperCodeGeneration

setup(
    name='pythonmatsim',
    version='1.0-SNAPSHOT',
    package_dir={'': '', '': 'generated-code'},
    packages=['pythonmatsim'],
    include_package_data=True,
    url='',
    license='GNU GPL 3.0',
    author='Thibaut Dubernet',
    author_email='thibaut.dubernet@ivt.baug.ethz.ch',
    description='A package to use MATSim from Python',
    cmdclass={
      'codegen': JavaWrapperCodeGeneration
    },
)
