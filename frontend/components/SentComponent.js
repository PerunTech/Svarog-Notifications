import { React, connect, PropTypes, ComponentManager, ExportableGrid } from 'perun-core'
import MessagesComponent from './MessagesComponent';
import { iconManager } from './svgHolder';
const tableName = 'SUBJECT'

class SentComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      objIdState: '',
      objectTypeState: '',
      showGrid: false,
      gridId: `${tableName}_SENT_GRID`,
      type: 'Sent'
    };

  }

  componentDidMount() {
    this.showSentGrid();
  }

  componentWillUnmount() {
    ComponentManager.cleanComponentReducerState(this.state.gridId)
  }


  showSentGrid = () => {
    const { gridId } = this.state
    let gridElementArr = []
    let htmlElement = <div className='context-menu-holder'>
      <p className='sent-paragraph'>Sent</p>
    </div>
    let grid = <ExportableGrid
      gridType={'READ_URL'}
      key={gridId}
      id={gridId}
      configTableName={'/SvarogNotificationsServices/getTableFieldList/%session/' + tableName}
      dataTableName={'/SvarogNotificationsServices/getSentSubjects/%session'}
      minHeight={640}
      onRowClickFunct={this.onSentRowClick}
      customClassName={'customGridClass'}
    />
    gridElementArr.push(htmlElement, grid)

    this.setState({ generateGridElement: gridElementArr, showGrid: true })
    ComponentManager.cleanComponentReducerState(gridId)
  }

  onSentRowClick = (id, idx, row) => {
    // const objId = `id=${row[`${tableName}.OBJECT_ID`]}`
    // const objectType = `type=${row[`${tableName}.OBJECT_TYPE`]}`
    const objId = row[`${tableName}.OBJECT_ID`]
    const objectType = row[`${tableName}.OBJECT_TYPE`]
    this.setState({ objIdState: objId, objectTypeState: objectType, showGrid: false })
    // hashHistory.push(`/main/message?${objId}&${objectType}`)
  }

  handleBack = () => {
    this.setState({ objIdState: '', objectTypeState: '', showGrid: true })
  }


  render() {
    const { generateGridElement, objIdState, objectTypeState, showGrid, type } = this.state
    return (
      <React.Fragment>
        {objIdState && objectTypeState && <MessagesComponent handleBack={this.handleBack} objId={objIdState} objType={objectTypeState} type={type} />}
        {showGrid && generateGridElement}
      </React.Fragment>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

SentComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(SentComponent)